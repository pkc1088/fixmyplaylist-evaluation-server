package evaluation.evaluationService.evaluation.domain.model;

import evaluation.evaluationService.evaluation.domain.model.enums.EvaluationLabel;
import evaluation.evaluationService.evaluation.domain.model.enums.EvaluationStatus;
import evaluation.evaluationService.evaluation.domain.model.vo.ReferenceTrace;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class EvaluationCaseTest {

    @Test
    void createPendingValidatesIdentityAndTitlesAndTruncatesLongTitles() {
        assertThrows(IllegalArgumentException.class, () -> EvaluationCase.createPending(null, "Target", "Source"));
        assertThrows(IllegalArgumentException.class, () -> EvaluationCase.createPending("  ", "Target", "Source"));
        assertThrows(IllegalArgumentException.class, () -> EvaluationCase.createPending("a".repeat(101), "Target", "Source"));
        assertThrows(IllegalArgumentException.class, () -> EvaluationCase.createPending("id", null, "Source"));
        assertThrows(IllegalArgumentException.class, () -> EvaluationCase.createPending("id", "  ", "Source"));
        assertThrows(IllegalArgumentException.class, () -> EvaluationCase.createPending("id", "Target", null));
        assertThrows(IllegalArgumentException.class, () -> EvaluationCase.createPending("id", "Target", "  "));

        String longTitle = "x".repeat(300);
        EvaluationCase evaluationCase = EvaluationCase.createPending("a".repeat(100), longTitle, longTitle);

        assertEquals(EvaluationStatus.PENDING, evaluationCase.getEvaluationStatus());
        assertEquals(0, evaluationCase.getRetryCount());
        assertEquals(255, evaluationCase.getTargetTitle().length());
        assertEquals(255, evaluationCase.getSourceTitle().length());
        assertNotNull(evaluationCase.getCreatedAt());
        assertTrue(evaluationCase.getRetrievedInfo().isEmpty());
    }

    @Test
    void handleFailureIncrementsRetryCountAndSetsFailedUntilRetryLimit() {
        EvaluationCase evaluationCase = EvaluationCase.createPending(
                "evaluation-1",
                "Replacement title",
                "Original title"
        );

        evaluationCase.handleFailure();

        assertEquals(1, evaluationCase.getRetryCount());
        assertEquals(EvaluationStatus.FAILED, evaluationCase.getEvaluationStatus());

        evaluationCase.handleFailure();

        assertEquals(2, evaluationCase.getRetryCount());
        assertEquals(EvaluationStatus.FAILED, evaluationCase.getEvaluationStatus());
    }

    @Test
    void handleFailureSetsDeadWhenRetryLimitIsReached() {
        EvaluationCase evaluationCase = EvaluationCase.createPending(
                "evaluation-2",
                "Replacement title",
                "Original title"
        );

        evaluationCase.handleFailure();
        evaluationCase.handleFailure();
        evaluationCase.handleFailure();

        assertEquals(2, evaluationCase.getRetryCount());
        assertEquals(EvaluationStatus.DEAD, evaluationCase.getEvaluationStatus());
    }

    @Test
    void applyAiEvaluationTransitionsPendingAndFailedCases() {
        EvaluationCase pendingCase = EvaluationCase.createPending("evaluation-ai-1", "Target", "Source");
        List<ReferenceTrace> traces = List.of(new ReferenceTrace("ref-1", 0.91));

        pendingCase.applyAiEvaluation(EvaluationLabel.SUCCESS, 0.92, traces);

        assertEquals(EvaluationStatus.AI_EVALUATED, pendingCase.getEvaluationStatus());
        assertEquals(EvaluationLabel.SUCCESS, pendingCase.getAiLabel());
        assertEquals(0.92, pendingCase.getAiConfidence());
        assertEquals(traces, pendingCase.getRetrievedInfo());
        assertNotNull(pendingCase.getEvaluatedAt());

        EvaluationCase failedCase = EvaluationCase.createPending("evaluation-ai-2", "Target", "Source");
        failedCase.handleFailure();

        failedCase.applyAiEvaluation(EvaluationLabel.FAIL, 0.65, List.of());

        assertEquals(EvaluationStatus.AI_EVALUATED, failedCase.getEvaluationStatus());
        assertEquals(EvaluationLabel.FAIL, failedCase.getAiLabel());
        assertEquals(0.65, failedCase.getAiConfidence());
        assertEquals(1, failedCase.getRetryCount());
    }

    @Test
    void handleFailureAndApplyAiEvaluationRejectNonEvaluableStates() {
        EvaluationCase aiEvaluated = EvaluationCase.createPending("evaluation-guard-1", "Target", "Source");
        aiEvaluated.applyAiEvaluation(EvaluationLabel.SUCCESS, 0.9, List.of());

        assertThrows(IllegalStateException.class, aiEvaluated::handleFailure);
        assertThrows(IllegalStateException.class, () -> aiEvaluated.applyAiEvaluation(EvaluationLabel.FAIL, 0.5, List.of()));

        EvaluationCase humanReviewed = EvaluationCase.createPending("evaluation-guard-2", "Target", "Source");
        humanReviewed.applyAiEvaluation(EvaluationLabel.SUCCESS, 0.9, List.of());
        humanReviewed.applyHumanReview(EvaluationLabel.SUCCESS, "검수 완료");

        assertThrows(IllegalStateException.class, humanReviewed::handleFailure);
        assertThrows(IllegalStateException.class, () -> humanReviewed.applyAiEvaluation(EvaluationLabel.FAIL, 0.5, List.of()));

        EvaluationCase dead = EvaluationCase.createPending("evaluation-guard-3", "Target", "Source");
        dead.handleFailure();
        dead.handleFailure();
        dead.handleFailure();

        assertThrows(IllegalStateException.class, dead::handleFailure);
        assertThrows(IllegalStateException.class, () -> dead.applyAiEvaluation(EvaluationLabel.SUCCESS, 0.8, List.of()));
        assertEquals(EvaluationStatus.DEAD, dead.getEvaluationStatus());
    }

    @Test
    void humanReviewTransitionsAiEvaluatedCaseAndStoresReviewData() {
        EvaluationCase evaluationCase = EvaluationCase.createPending("evaluation-3", "Target", "Source");

        evaluationCase.applyAiEvaluation(EvaluationLabel.SUCCESS, 0.8, null);

        evaluationCase.applyHumanReview(EvaluationLabel.FAIL, "직접 확인한 결과 불일치");

        assertEquals(EvaluationStatus.HUMAN_REVIEWED, evaluationCase.getEvaluationStatus());
        assertEquals(EvaluationLabel.FAIL, evaluationCase.getHumanLabel());
        assertEquals("직접 확인한 결과 불일치", evaluationCase.getHumanReason());
        assertNotNull(evaluationCase.getReviewedAt());
    }

    @Test
    void humanReviewAcceptsBoundaryReasonLength() {
        EvaluationCase evaluationCase = EvaluationCase.createPending("evaluation-boundary", "Target", "Source");
        evaluationCase.applyAiEvaluation(EvaluationLabel.SUCCESS, 0.8, List.of());
        String maxReason = "r".repeat(4000);

        evaluationCase.applyHumanReview(EvaluationLabel.SUCCESS, maxReason);

        assertEquals(EvaluationStatus.HUMAN_REVIEWED, evaluationCase.getEvaluationStatus());
        assertEquals(maxReason, evaluationCase.getHumanReason());
    }

    @Test
    void humanReviewRequiresAiEvaluatedStateAndValidReviewFields() {
        EvaluationCase pending = EvaluationCase.createPending("evaluation-4", "Target", "Source");
        assertThrows(IllegalStateException.class, () -> pending.applyHumanReview(EvaluationLabel.SUCCESS, "reason"));

        EvaluationCase failed = EvaluationCase.createPending("evaluation-failed", "Target", "Source");
        failed.handleFailure();
        assertThrows(IllegalStateException.class, () -> failed.applyHumanReview(EvaluationLabel.SUCCESS, "reason"));

        EvaluationCase dead = EvaluationCase.createPending("evaluation-dead", "Target", "Source");
        dead.handleFailure();
        dead.handleFailure();
        dead.handleFailure();
        assertThrows(IllegalStateException.class, () -> dead.applyHumanReview(EvaluationLabel.SUCCESS, "reason"));

        EvaluationCase aiEvaluated = EvaluationCase.createPending("evaluation-5", "Target", "Source");
        aiEvaluated.applyAiEvaluation(EvaluationLabel.SUCCESS, 0.8, null);

        assertThrows(IllegalArgumentException.class, () -> aiEvaluated.applyHumanReview(null, "reason"));
        assertThrows(IllegalArgumentException.class, () -> aiEvaluated.applyHumanReview(EvaluationLabel.FAIL, null));
        assertThrows(IllegalArgumentException.class, () -> aiEvaluated.applyHumanReview(EvaluationLabel.FAIL, " "));
        assertThrows(IllegalArgumentException.class, () -> aiEvaluated.applyHumanReview(EvaluationLabel.FAIL, "r".repeat(4001)));

        aiEvaluated.applyHumanReview(EvaluationLabel.SUCCESS, "ok");
        assertThrows(IllegalStateException.class, () -> aiEvaluated.applyHumanReview(EvaluationLabel.SUCCESS, "ok"));
    }

    @Test
    void reconstituteRestoresFieldsAndAppliesDefaultsForNullableMetadata() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 2, 3, 4, 5);
        LocalDateTime evaluatedAt = LocalDateTime.of(2026, 1, 2, 4, 0, 0);
        LocalDateTime reviewedAt = LocalDateTime.of(2026, 1, 2, 5, 0, 0);
        List<ReferenceTrace> traces = List.of(new ReferenceTrace("ref-1", 0.77));

        EvaluationCase full = EvaluationCase.reconstitute(
                "eval-full", "Target", "Source",
                EvaluationLabel.FAIL, 0.42, EvaluationStatus.HUMAN_REVIEWED, 1,
                EvaluationLabel.SUCCESS, "reason",
                createdAt, evaluatedAt, reviewedAt, traces
        );

        assertEquals("eval-full", full.getEvaluationCaseId());
        assertEquals(createdAt, full.getCreatedAt());
        assertEquals(evaluatedAt, full.getEvaluatedAt());
        assertEquals(reviewedAt, full.getReviewedAt());
        assertEquals(traces, full.getRetrievedInfo());

        EvaluationCase defaulted = EvaluationCase.reconstitute(
                "eval-default", "Target", "Source",
                null, null, EvaluationStatus.PENDING, null,
                null, null, null, null, null, null
        );

        assertEquals(0, defaulted.getRetryCount());
        assertNotNull(defaulted.getCreatedAt());
        assertNotNull(defaulted.getRetrievedInfo());
        assertTrue(defaulted.getRetrievedInfo().isEmpty());
    }
}
