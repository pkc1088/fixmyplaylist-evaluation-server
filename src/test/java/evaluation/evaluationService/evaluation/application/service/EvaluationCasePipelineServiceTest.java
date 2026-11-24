package evaluation.evaluationService.evaluation.application.service;

import evaluation.evaluationService.evaluation.application.port.in.dto.ReviewCaseResponse;
import evaluation.evaluationService.evaluation.application.port.out.*;
import evaluation.evaluationService.evaluation.application.port.out.dto.EvaluationResult;
import evaluation.evaluationService.evaluation.application.port.out.dto.RecoveryCompletedEvent;
import evaluation.evaluationService.evaluation.application.port.out.dto.VectorSearchResult;
import evaluation.evaluationService.evaluation.domain.model.EvaluationCase;
import evaluation.evaluationService.evaluation.domain.model.ReferenceCase;
import evaluation.evaluationService.evaluation.domain.model.enums.EvaluationLabel;
import evaluation.evaluationService.evaluation.domain.model.enums.EvaluationStatus;
import evaluation.evaluationService.evaluation.domain.model.vo.RetrievedCase;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class EvaluationCasePipelineServiceTest {

    @Mock private EvaluationCaseInboxService evaluationCaseInboxService;
    @Mock private RetrieveReferenceCasePort retrieveReferenceCasePort;
    @Mock private QueryEvaluationCasePort queryEvaluationCasePort;
    @Mock private QueryReferenceCasePort queryReferenceCasePort;
    @Mock private EvaluateRecoveryPort evaluateRecoveryPort;
    @Mock private MessagePullPort messagePullPort;
    @Mock private DlqPort dlqPort;
    @InjectMocks private EvaluationCasePipelineService service;

    @Test
    void evaluatesWithMatchingRetrievedCasesAndPersistsAiResultAndTraces() {
        EvaluationCase evaluation = EvaluationCase.createPending("event-1", "Target", "Source");
        ReferenceCase ref = reference("ref-1");

        when(queryEvaluationCasePort.loadPendingEvaluation())
                .thenReturn(List.of(evaluation));
        when(retrieveReferenceCasePort.retrieveIds(evaluation, ReferenceCase.RAG_TOP_K))
                .thenReturn(List.of(
                        new VectorSearchResult("ref-1", 0.91),
                        new VectorSearchResult("missing-reference", 0.72)));
        when(queryReferenceCasePort.loadByIds(List.of("ref-1", "missing-reference")))
                .thenReturn(List.of(ref));
        when(evaluateRecoveryPort.evaluateWithRag(eq(evaluation), anyList()))
                .thenAnswer(invocation -> {
                    List<RetrievedCase> examples = invocation.getArgument(1);
                    assertEquals(1, examples.size());
                    assertEquals(ref, examples.get(0).referenceCase());
                    assertEquals(0.91, examples.get(0).similarityScore());
                    return new EvaluationResult(EvaluationLabel.SUCCESS, 0.95);
                });

        service.evaluatePendingCases();

        assertEquals(EvaluationStatus.AI_EVALUATED, evaluation.getEvaluationStatus());
        assertEquals(EvaluationLabel.SUCCESS, evaluation.getAiLabel());
        assertEquals(0.95, evaluation.getAiConfidence());
        assertEquals(1, evaluation.getRetrievedInfo().size());
        assertEquals("ref-1", evaluation.getRetrievedInfo().get(0).referenceCaseId());
        assertEquals(0.91, evaluation.getRetrievedInfo().get(0).similarityScore());
        verify(evaluationCaseInboxService).updateEvaluationResult(evaluation);
    }

    @Test
    void continuesAfterEvaluationFailureAndRecordsFailureState() {
        EvaluationCase failedCase = EvaluationCase.createPending("event-2", "Failed target", "Failed source");
        EvaluationCase succeededCase = EvaluationCase.createPending("event-3", "Succeeded target", "Succeeded source");
        ReferenceCase ref = reference("ref-1");

        when(queryEvaluationCasePort.loadPendingEvaluation())
                .thenReturn(List.of(failedCase, succeededCase));
        when(retrieveReferenceCasePort.retrieveIds(failedCase, ReferenceCase.RAG_TOP_K))
                .thenThrow(new IllegalStateException("search failed"));
        when(retrieveReferenceCasePort.retrieveIds(succeededCase, ReferenceCase.RAG_TOP_K))
                .thenReturn(List.of(new VectorSearchResult("ref-1", 0.91)));
        when(queryReferenceCasePort.loadByIds(List.of("ref-1")))
                .thenReturn(List.of(ref));
        when(evaluateRecoveryPort.evaluateWithRag(eq(succeededCase), anyList()))
                .thenReturn(new EvaluationResult(EvaluationLabel.SUCCESS, 0.95));

        service.evaluatePendingCases();

        assertEquals(EvaluationStatus.FAILED, failedCase.getEvaluationStatus());
        assertEquals(1, failedCase.getRetryCount());
        assertEquals(EvaluationStatus.AI_EVALUATED, succeededCase.getEvaluationStatus());
        assertEquals(EvaluationLabel.SUCCESS, succeededCase.getAiLabel());
        assertEquals(0.95, succeededCase.getAiConfidence());

        verify(evaluationCaseInboxService).updateEvaluationResult(failedCase);
        verify(evaluationCaseInboxService).updateEvaluationResult(succeededCase);
        verify(evaluateRecoveryPort, never()).evaluateWithRag(eq(failedCase), anyList());
    }

    @Test
    void dlqsInvalidMessagesWhilePersistingValidEventsFromPull() throws Exception {
        when(messagePullPort.pullAndProcess(any()))
                .thenAnswer(invocation -> {
                    EventProcessor processor = invocation.getArgument(0);
                    processor.process(new RecoveryCompletedEvent("event-3", "Target", "Source"));
                    processor.onFail("raw-invalid-json");
                    return 1;
                });
        when(evaluationCaseInboxService.saveToInboxIdempotent(any()))
                .thenReturn(true);
        when(queryEvaluationCasePort.loadPendingEvaluation())
                .thenReturn(List.of());

        service.evaluatePendingCases();

        ArgumentCaptor<RecoveryCompletedEvent> captor = ArgumentCaptor.forClass(RecoveryCompletedEvent.class);
        verify(evaluationCaseInboxService).saveToInboxIdempotent(captor.capture());
        assertEquals("event-3", captor.getValue().eventId());
        verify(dlqPort).sendToDlq("raw-invalid-json");
    }

    @Test
    void mapsPendingCasesIntoReviewResponses() {
        EvaluationCase evaluation = EvaluationCase.createPending("event-4", "Target", "Source");
        evaluation.applyAiEvaluation(EvaluationLabel.FAIL, 0.4, List.of());

        when(queryEvaluationCasePort.loadPendingReview())
                .thenReturn(List.of(evaluation));

        List<ReviewCaseResponse> result = service.loadPendingReviewCases();

        assertEquals(1, result.size());
        assertEquals("event-4", result.get(0).evaluationCaseId());
        assertEquals("Target", result.get(0).targetTitle());
        assertEquals("Source", result.get(0).sourceTitle());
        assertEquals(EvaluationLabel.FAIL, result.get(0).aiLabel());
        assertEquals(0.4, result.get(0).aiConfidence());
        assertNotNull(result.get(0).evaluatedAt());
    }

    private ReferenceCase reference(String id) {
        return ReferenceCase.create(id, "Old target", "Old source", EvaluationLabel.FAIL, "human reason");
    }
}
