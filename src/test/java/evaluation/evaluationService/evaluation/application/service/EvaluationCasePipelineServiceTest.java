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
    void evaluatesWithMatchingRetrievedCasesPreservingSearchOrderAndPersistsTraces() {
        EvaluationCase evaluation = EvaluationCase.createPending("event-1", "Target", "Source");
        ReferenceCase ref1 = reference("ref-1");
        ReferenceCase ref2 = reference("ref-2");

        when(queryEvaluationCasePort.loadPendingEvaluation())
                .thenReturn(List.of(evaluation));
        when(retrieveReferenceCasePort.retrieveIds(evaluation, ReferenceCase.RAG_TOP_K))
                .thenReturn(List.of(
                        new VectorSearchResult("ref-2", 0.94),
                        new VectorSearchResult("missing-reference", 0.80),
                        new VectorSearchResult("ref-1", 0.72)));
        when(queryReferenceCasePort.loadByIds(List.of("ref-2", "missing-reference", "ref-1")))
                .thenReturn(List.of(ref1, ref2));
        when(evaluateRecoveryPort.evaluateWithRag(eq(evaluation), anyList()))
                .thenAnswer(invocation -> {
                    List<RetrievedCase> examples = invocation.getArgument(1);
                    assertEquals(2, examples.size());
                    assertEquals(ref2, examples.get(0).referenceCase());
                    assertEquals(0.94, examples.get(0).similarityScore());
                    assertEquals(ref1, examples.get(1).referenceCase());
                    assertEquals(0.72, examples.get(1).similarityScore());
                    return new EvaluationResult(EvaluationLabel.SUCCESS, 0.95);
                });

        service.evaluatePendingCases();

        assertEquals(EvaluationStatus.AI_EVALUATED, evaluation.getEvaluationStatus());
        assertEquals(EvaluationLabel.SUCCESS, evaluation.getAiLabel());
        assertEquals(0.95, evaluation.getAiConfidence());
        assertEquals(2, evaluation.getRetrievedInfo().size());
        assertEquals("ref-2", evaluation.getRetrievedInfo().get(0).referenceCaseId());
        assertEquals(0.94, evaluation.getRetrievedInfo().get(0).similarityScore());
        assertEquals("ref-1", evaluation.getRetrievedInfo().get(1).referenceCaseId());
        assertEquals(0.72, evaluation.getRetrievedInfo().get(1).similarityScore());
        verify(evaluationCaseInboxService).updateEvaluationResult(evaluation);
    }

    @Test
    void evaluatesWithEmptyExamplesWhenVectorSearchReturnsNoMatches() {
        EvaluationCase evaluation = EvaluationCase.createPending("event-empty-rag", "Target", "Source");

        when(queryEvaluationCasePort.loadPendingEvaluation()).thenReturn(List.of(evaluation));
        when(retrieveReferenceCasePort.retrieveIds(evaluation, ReferenceCase.RAG_TOP_K)).thenReturn(List.of());
        when(queryReferenceCasePort.loadByIds(List.of())).thenReturn(List.of());
        when(evaluateRecoveryPort.evaluateWithRag(evaluation, List.of()))
                .thenReturn(new EvaluationResult(EvaluationLabel.FAIL, 0.55));

        service.evaluatePendingCases();

        assertEquals(EvaluationStatus.AI_EVALUATED, evaluation.getEvaluationStatus());
        assertEquals(EvaluationLabel.FAIL, evaluation.getAiLabel());
        assertTrue(evaluation.getRetrievedInfo().isEmpty());
        verify(evaluationCaseInboxService).updateEvaluationResult(evaluation);
    }

    @Test
    void continuesAfterEvaluationFailureAndTransitionsToDeadOnThirdFailure() {
        EvaluationCase failedCase = EvaluationCase.createPending("event-2", "Failed target", "Failed source");
        EvaluationCase deadCandidate = EvaluationCase.createPending("event-dead", "Dead target", "Dead source");
        deadCandidate.handleFailure();
        deadCandidate.handleFailure();
        EvaluationCase succeededCase = EvaluationCase.createPending("event-3", "Succeeded target", "Succeeded source");
        ReferenceCase ref = reference("ref-1");

        when(queryEvaluationCasePort.loadPendingEvaluation())
                .thenReturn(List.of(failedCase, deadCandidate, succeededCase));
        when(retrieveReferenceCasePort.retrieveIds(failedCase, ReferenceCase.RAG_TOP_K))
                .thenThrow(new IllegalStateException("search failed"));
        when(retrieveReferenceCasePort.retrieveIds(deadCandidate, ReferenceCase.RAG_TOP_K))
                .thenReturn(List.of());
        when(retrieveReferenceCasePort.retrieveIds(succeededCase, ReferenceCase.RAG_TOP_K))
                .thenReturn(List.of(new VectorSearchResult("ref-1", 0.91)));
        when(queryReferenceCasePort.loadByIds(List.of())).thenReturn(List.of());
        when(queryReferenceCasePort.loadByIds(List.of("ref-1"))).thenReturn(List.of(ref));
        when(evaluateRecoveryPort.evaluateWithRag(eq(deadCandidate), anyList()))
                .thenThrow(new RuntimeException("llm error"));
        when(evaluateRecoveryPort.evaluateWithRag(eq(succeededCase), anyList()))
                .thenReturn(new EvaluationResult(EvaluationLabel.SUCCESS, 0.95));

        service.evaluatePendingCases();

        assertEquals(EvaluationStatus.FAILED, failedCase.getEvaluationStatus());
        assertEquals(1, failedCase.getRetryCount());
        assertEquals(EvaluationStatus.DEAD, deadCandidate.getEvaluationStatus());
        assertEquals(2, deadCandidate.getRetryCount());
        assertEquals(EvaluationStatus.AI_EVALUATED, succeededCase.getEvaluationStatus());
        assertEquals(EvaluationLabel.SUCCESS, succeededCase.getAiLabel());
        assertEquals(0.95, succeededCase.getAiConfidence());

        verify(evaluationCaseInboxService).updateEvaluationResult(failedCase);
        verify(evaluationCaseInboxService).updateEvaluationResult(deadCandidate);
        verify(evaluationCaseInboxService).updateEvaluationResult(succeededCase);
        verify(evaluateRecoveryPort, never()).evaluateWithRag(eq(failedCase), anyList());
    }

    @Test
    void continuesEvaluatingRemainingCasesWhenDatabaseUpdateFails() {
        EvaluationCase first = EvaluationCase.createPending("event-db-fail", "Target 1", "Source 1");
        EvaluationCase second = EvaluationCase.createPending("event-db-ok", "Target 2", "Source 2");

        when(queryEvaluationCasePort.loadPendingEvaluation()).thenReturn(List.of(first, second));
        when(retrieveReferenceCasePort.retrieveIds(any(), eq(ReferenceCase.RAG_TOP_K))).thenReturn(List.of());
        when(queryReferenceCasePort.loadByIds(List.of())).thenReturn(List.of());
        when(evaluateRecoveryPort.evaluateWithRag(any(), eq(List.of())))
                .thenReturn(new EvaluationResult(EvaluationLabel.SUCCESS, 0.9));
        doThrow(new RuntimeException("db timeout"))
                .when(evaluationCaseInboxService).updateEvaluationResult(first);

        assertDoesNotThrow(() -> service.evaluatePendingCases());

        assertEquals(EvaluationStatus.AI_EVALUATED, second.getEvaluationStatus());
        verify(evaluationCaseInboxService).updateEvaluationResult(first);
        verify(evaluationCaseInboxService).updateEvaluationResult(second);
    }

    @Test
    void dlqsInvalidMessagesWhilePersistingNewAndSkippingDuplicateEventsFromPull() throws Exception {
        when(messagePullPort.pullAndProcess(any()))
                .thenAnswer(invocation -> {
                    EventProcessor processor = invocation.getArgument(0);
                    processor.process(new RecoveryCompletedEvent("event-3", "Target", "Source"));
                    processor.process(new RecoveryCompletedEvent("event-dup", "Target", "Source"));
                    processor.onFail("raw-invalid-json");
                    return 2;
                });
        when(evaluationCaseInboxService.saveToInboxIdempotent(new RecoveryCompletedEvent("event-3", "Target", "Source")))
                .thenReturn(true);
        when(evaluationCaseInboxService.saveToInboxIdempotent(new RecoveryCompletedEvent("event-dup", "Target", "Source")))
                .thenReturn(false);
        when(queryEvaluationCasePort.loadPendingEvaluation())
                .thenReturn(List.of());

        service.evaluatePendingCases();

        ArgumentCaptor<RecoveryCompletedEvent> captor = ArgumentCaptor.forClass(RecoveryCompletedEvent.class);
        verify(evaluationCaseInboxService, times(2)).saveToInboxIdempotent(captor.capture());
        assertEquals(List.of("event-3", "event-dup"),
                captor.getAllValues().stream().map(RecoveryCompletedEvent::eventId).toList());
        verify(dlqPort, times(1)).sendToDlq("raw-invalid-json");
    }

    @Test
    void mapsPendingCasesIntoReviewResponsesAndHandlesEmptyList() {
        EvaluationCase evaluation = EvaluationCase.createPending("event-4", "Target", "Source");
        evaluation.applyAiEvaluation(EvaluationLabel.FAIL, 0.4, List.of());

        when(queryEvaluationCasePort.loadPendingReview())
                .thenReturn(List.of(evaluation), List.of());

        List<ReviewCaseResponse> result = service.loadPendingReviewCases();

        assertEquals(1, result.size());
        assertEquals("event-4", result.get(0).evaluationCaseId());
        assertEquals("Target", result.get(0).targetTitle());
        assertEquals("Source", result.get(0).sourceTitle());
        assertEquals(EvaluationLabel.FAIL, result.get(0).aiLabel());
        assertEquals(0.4, result.get(0).aiConfidence());
        assertNotNull(result.get(0).evaluatedAt());

        assertTrue(service.loadPendingReviewCases().isEmpty());
    }

    private ReferenceCase reference(String id) {
        return ReferenceCase.create(id, "Old target", "Old source", EvaluationLabel.FAIL, "human reason");
    }
}
