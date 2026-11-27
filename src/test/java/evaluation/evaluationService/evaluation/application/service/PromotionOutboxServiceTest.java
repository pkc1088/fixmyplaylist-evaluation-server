package evaluation.evaluationService.evaluation.application.service;

import evaluation.evaluationService.evaluation.application.port.out.CommandEvaluationCasePort;
import evaluation.evaluationService.evaluation.application.port.out.CommandReferenceCasePort;
import evaluation.evaluationService.evaluation.application.port.out.PromotionOutboxPort;
import evaluation.evaluationService.evaluation.application.port.out.QueryEvaluationCasePort;
import evaluation.evaluationService.evaluation.domain.exception.EvaluationCaseNotFoundException;
import evaluation.evaluationService.evaluation.domain.model.EvaluationCase;
import evaluation.evaluationService.evaluation.domain.model.ReferenceCase;
import evaluation.evaluationService.evaluation.domain.model.enums.EvaluationLabel;
import evaluation.evaluationService.evaluation.domain.model.enums.EvaluationStatus;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class PromotionOutboxServiceTest {

    @Mock private CommandEvaluationCasePort commandEvaluationCasePort;
    @Mock private CommandReferenceCasePort commandReferenceCasePort;
    @Mock private QueryEvaluationCasePort queryEvaluationCasePort;
    @Mock private PromotionOutboxPort promotionOutboxPort;

    @InjectMocks private PromotionOutboxService service;

    @Test
    void reviewAndQueueReviewsEvaluationAndQueuesReferenceUsingSameId() {
        EvaluationCase evaluationCase = aiEvaluatedCase("case-1");

        when(queryEvaluationCasePort.loadById("case-1"))
                .thenReturn(evaluationCase);

        Optional<String> result = service.reviewAndQueue("case-1", EvaluationLabel.SUCCESS, "검수 근거");

        assertEquals(Optional.of("case-1"), result);
        assertEquals(EvaluationStatus.HUMAN_REVIEWED, evaluationCase.getEvaluationStatus());
        assertEquals(EvaluationLabel.SUCCESS, evaluationCase.getHumanLabel());
        assertEquals("검수 근거", evaluationCase.getHumanReason());

        ArgumentCaptor<ReferenceCase> referenceCaptor = ArgumentCaptor.forClass(ReferenceCase.class);
        InOrder order = inOrder(commandReferenceCasePort, commandEvaluationCasePort, promotionOutboxPort);
        order.verify(commandReferenceCasePort).save(referenceCaptor.capture());
        order.verify(commandEvaluationCasePort).update(evaluationCase);
        order.verify(promotionOutboxPort).enqueue("case-1");

        ReferenceCase referenceCase = referenceCaptor.getValue();
        assertEquals("case-1", referenceCase.getReferenceCaseId());
        assertEquals("Target title", referenceCase.getTargetTitle());
        assertEquals("Source title", referenceCase.getSourceTitle());
        assertEquals(EvaluationLabel.SUCCESS, referenceCase.getHumanLabel());
        assertEquals("검수 근거", referenceCase.getHumanReason());
    }

    @Test
    void sameReviewRetriesOnlyWhenOutboxNeedsIndexing() {
        EvaluationCase evaluationCase = reviewedCase("case-2", EvaluationLabel.FAIL, "사람 판단");

        when(queryEvaluationCasePort.loadById("case-2"))
                .thenReturn(evaluationCase);
        when(promotionOutboxPort.needsIndexing("case-2"))
                .thenReturn(true, false);

        assertEquals(Optional.of("case-2"), service.reviewAndQueue("case-2", EvaluationLabel.FAIL, "사람 판단"));
        assertEquals(Optional.empty(), service.reviewAndQueue("case-2", EvaluationLabel.FAIL, "사람 판단"));

        verify(promotionOutboxPort, times(2)).needsIndexing("case-2");
        verifyNoInteractions(commandReferenceCasePort, commandEvaluationCasePort);
        verify(promotionOutboxPort, never()).enqueue(anyString());
    }

    @Test
    void differentReviewForAlreadyReviewedCaseIsRejectedWithoutMutation() {
        EvaluationCase evaluationCase = reviewedCase("case-3", EvaluationLabel.SUCCESS, "기존 근거");

        when(queryEvaluationCasePort.loadById("case-3")).thenReturn(evaluationCase);

        assertThrows(IllegalStateException.class,
                () -> service.reviewAndQueue("case-3", EvaluationLabel.FAIL, "기존 근거"));
        assertThrows(IllegalStateException.class,
                () -> service.reviewAndQueue("case-3", EvaluationLabel.SUCCESS, "새 근거"));
        assertThrows(IllegalStateException.class,
                () -> service.reviewAndQueue("case-3", EvaluationLabel.SUCCESS, null));

        verifyNoInteractions(commandReferenceCasePort, commandEvaluationCasePort, promotionOutboxPort);
        assertEquals(EvaluationLabel.SUCCESS, evaluationCase.getHumanLabel());
        assertEquals("기존 근거", evaluationCase.getHumanReason());
    }

    @Test
    void reviewOfCaseNotInAiEvaluatedStateIsRejected() {
        EvaluationCase pending = EvaluationCase.createPending("case-pending", "Target title", "Source title");
        EvaluationCase failed = EvaluationCase.createPending("case-failed", "Target title", "Source title");
        failed.handleFailure();
        EvaluationCase dead = EvaluationCase.createPending("case-dead", "Target title", "Source title");
        dead.handleFailure();
        dead.handleFailure();
        dead.handleFailure();

        when(queryEvaluationCasePort.loadById("case-pending")).thenReturn(pending);
        when(queryEvaluationCasePort.loadById("case-failed")).thenReturn(failed);
        when(queryEvaluationCasePort.loadById("case-dead")).thenReturn(dead);

        assertThrows(IllegalStateException.class,
                () -> service.reviewAndQueue("case-pending", EvaluationLabel.FAIL, "사람 판단"));
        assertThrows(IllegalStateException.class,
                () -> service.reviewAndQueue("case-failed", EvaluationLabel.FAIL, "사람 판단"));
        assertThrows(IllegalStateException.class,
                () -> service.reviewAndQueue("case-dead", EvaluationLabel.FAIL, "사람 판단"));

        verifyNoInteractions(commandReferenceCasePort, commandEvaluationCasePort, promotionOutboxPort);
    }

    @Test
    void rejectsInvalidReviewInputsAndMissingCaseWithoutMutatingPorts() {
        when(queryEvaluationCasePort.loadById("missing"))
                .thenThrow(new EvaluationCaseNotFoundException("missing"));
        assertThrows(EvaluationCaseNotFoundException.class,
                () -> service.reviewAndQueue("missing", EvaluationLabel.SUCCESS, "근거"));

        EvaluationCase aiEvaluated = aiEvaluatedCase("case-invalid");
        when(queryEvaluationCasePort.loadById("case-invalid")).thenReturn(aiEvaluated);

        assertThrows(IllegalArgumentException.class,
                () -> service.reviewAndQueue("case-invalid", null, "근거"));
        assertThrows(IllegalArgumentException.class,
                () -> service.reviewAndQueue("case-invalid", EvaluationLabel.SUCCESS, " "));
        assertThrows(IllegalArgumentException.class,
                () -> service.reviewAndQueue("case-invalid", EvaluationLabel.SUCCESS, "r".repeat(4001)));

        verifyNoInteractions(commandReferenceCasePort, commandEvaluationCasePort, promotionOutboxPort);
    }

    private EvaluationCase aiEvaluatedCase(String id) {
        EvaluationCase evaluationCase = EvaluationCase.createPending(id, "Target title", "Source title");
        evaluationCase.applyAiEvaluation(EvaluationLabel.FAIL, 0.7, null);
        return evaluationCase;
    }

    private EvaluationCase reviewedCase(String id, EvaluationLabel label, String reason) {
        EvaluationCase evaluationCase = aiEvaluatedCase(id);
        evaluationCase.applyHumanReview(label, reason);
        return evaluationCase;
    }
}
