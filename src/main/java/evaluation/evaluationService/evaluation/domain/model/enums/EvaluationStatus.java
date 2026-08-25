package evaluation.evaluationService.evaluation.domain.model.enums;

public enum EvaluationStatus {
    PENDING,
    FAILED,
    DEAD,
    AI_EVALUATED,
    HUMAN_REVIEWED // Promotion: AI_EVALUATED -> HUMAN_REVIEWED
}
