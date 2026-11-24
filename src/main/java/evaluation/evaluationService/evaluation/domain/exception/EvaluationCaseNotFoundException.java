package evaluation.evaluationService.evaluation.domain.exception;

public class EvaluationCaseNotFoundException extends RuntimeException {

    public EvaluationCaseNotFoundException(String evaluationCaseId) {
        super("EvaluationCase not found: " + evaluationCaseId);
    }
}
