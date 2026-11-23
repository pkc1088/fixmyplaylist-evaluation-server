package evaluation.evaluationService.evaluation.application.port.in;

import evaluation.evaluationService.evaluation.application.port.in.dto.ReviewCaseResponse;

import java.util.List;

public interface EvaluationCaseUseCase {

    void evaluatePendingCases();

    List<ReviewCaseResponse> loadPendingReviewCases();
}
