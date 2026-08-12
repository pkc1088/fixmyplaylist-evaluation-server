package evaluation.evaluationService.evaluation.domain.model.vo;

import evaluation.evaluationService.evaluation.domain.model.ReferenceCase;

public record RetrievedCase(
        ReferenceCase referenceCase,
        double score
) {
}