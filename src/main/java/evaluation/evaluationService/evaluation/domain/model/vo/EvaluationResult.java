package evaluation.evaluationService.evaluation.domain.model.vo;

import evaluation.evaluationService.evaluation.domain.model.enums.HumanLabel;

public record EvaluationResult(
        HumanLabel label,
        double confidence,
        String reason
) {
}
