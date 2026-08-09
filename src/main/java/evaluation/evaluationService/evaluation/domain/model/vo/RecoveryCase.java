package evaluation.evaluationService.evaluation.domain.model.vo;

import evaluation.evaluationService.evaluation.domain.model.enums.HumanLabel;

public record RecoveryCase(
        String id,
        String targetTitle,
        String sourceTitle,
        HumanLabel humanLabel,
        String humanReason
) {
}
