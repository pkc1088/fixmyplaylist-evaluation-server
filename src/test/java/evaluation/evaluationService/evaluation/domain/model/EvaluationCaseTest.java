package evaluation.evaluationService.evaluation.domain.model;

import evaluation.evaluationService.evaluation.domain.model.enums.EvaluationStatus;
import evaluation.evaluationService.evaluation.domain.model.enums.EvaluationLabel;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Tag("unit")
class EvaluationCaseTest {

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
    void humanReviewRequiresAiEvaluatedStateAndValidReviewFields() {
        EvaluationCase pending = EvaluationCase.createPending("evaluation-4", "Target", "Source");

        assertThrows(IllegalStateException.class, () -> pending.applyHumanReview(EvaluationLabel.SUCCESS, "reason"));

        EvaluationCase aiEvaluated = EvaluationCase.createPending("evaluation-5", "Target", "Source");

        aiEvaluated.applyAiEvaluation(EvaluationLabel.SUCCESS, 0.8, null);

        assertThrows(IllegalArgumentException.class, () -> aiEvaluated.applyHumanReview(null, "reason"));
        assertThrows(IllegalArgumentException.class, () -> aiEvaluated.applyHumanReview(EvaluationLabel.FAIL, " "));
        assertThrows(IllegalArgumentException.class, () -> aiEvaluated.applyHumanReview(EvaluationLabel.FAIL, "r".repeat(4001)));
    }
}
