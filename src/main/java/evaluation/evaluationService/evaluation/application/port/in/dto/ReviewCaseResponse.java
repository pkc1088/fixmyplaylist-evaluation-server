package evaluation.evaluationService.evaluation.application.port.in.dto;

import evaluation.evaluationService.evaluation.domain.model.enums.EvaluationLabel;

import java.time.LocalDateTime;

public record ReviewCaseResponse(
        String evaluationCaseId,
        String targetTitle,
        String sourceTitle,
        EvaluationLabel aiLabel,
        Double aiConfidence,
        LocalDateTime evaluatedAt
) {
}
