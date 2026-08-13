package evaluation.evaluationService.evaluation.application.port.out.dto;

public record VectorSearchResult(
        String referenceCaseId,
        Double confidence
) {
}
