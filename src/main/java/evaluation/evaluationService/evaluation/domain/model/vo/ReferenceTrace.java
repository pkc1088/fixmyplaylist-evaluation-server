package evaluation.evaluationService.evaluation.domain.model.vo;

public record ReferenceTrace(
        String referenceCaseId,
        Double similarityScore
) {
}