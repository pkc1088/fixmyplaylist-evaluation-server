package evaluation.evaluationService.evaluation.domain.model;

import evaluation.evaluationService.evaluation.domain.model.enums.EvaluationStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
