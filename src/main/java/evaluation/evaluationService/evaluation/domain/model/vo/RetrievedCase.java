package evaluation.evaluationService.evaluation.domain.model.vo;

public record RetrievedCase(
        RecoveryCase recoveryCase,
        double score
) {
}