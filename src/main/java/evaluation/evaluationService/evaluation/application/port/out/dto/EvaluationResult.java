package evaluation.evaluationService.evaluation.application.port.out.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import evaluation.evaluationService.evaluation.domain.model.enums.EvaluationLabel;

public record EvaluationResult(

        @JsonProperty(required = true)
        EvaluationLabel label,

        @JsonProperty(required = true)
        double confidence
) {
}
