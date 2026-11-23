package evaluation.evaluationService.evaluation.application.port.in;

import evaluation.evaluationService.evaluation.domain.model.enums.EvaluationLabel;
import evaluation.evaluationService.evaluation.application.port.in.dto.IndexRetryResult;

public interface PromotionUseCase {

    IndexRetryResult promote(String evaluationCaseId, EvaluationLabel humanLabel, String humanReason);

    IndexRetryResult retryPendingIndexing();
}
