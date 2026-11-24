package evaluation.evaluationService.evaluation.adapter.in.web.dto;

import evaluation.evaluationService.evaluation.domain.model.enums.EvaluationLabel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PromotionRequest(
        @NotNull EvaluationLabel humanLabel,
        @NotBlank @Size(max = 4000) String humanReason
) {
}
