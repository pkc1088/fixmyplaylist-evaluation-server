package evaluation.evaluationService.evaluation.application.port.in.dto;

public record IndexRetryResult(
        int indexed,
        int failed
) {
}
