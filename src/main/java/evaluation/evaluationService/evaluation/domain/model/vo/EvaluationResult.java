package evaluation.evaluationService.evaluation.domain.model.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import evaluation.evaluationService.evaluation.domain.model.enums.HumanLabel;

public record EvaluationResult(

        @JsonProperty(required = true)
        HumanLabel label,

        @JsonProperty(required = true)
        double confidence
) {
}
