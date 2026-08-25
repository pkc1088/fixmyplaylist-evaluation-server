package evaluation.evaluationService.evaluation.adapter.out.persistence;

import evaluation.evaluationService.evaluation.domain.model.EvaluationCase;
import org.springframework.stereotype.Component;

@Component
public class EvaluationCaseMapper {

    public EvaluationCaseJpaEntity toEntity(EvaluationCase domain, boolean isNew) {
        if (domain == null) return null;

        return EvaluationCaseJpaEntity.builder()
                .evaluationCaseId(domain.getEvaluationCaseId())
                .targetTitle(domain.getTargetTitle())
                .sourceTitle(domain.getSourceTitle())
                .aiLabel(domain.getAiLabel())
                .aiConfidence(domain.getAiConfidence())
                .evaluationStatus(domain.getEvaluationStatus())
                .retryCount(domain.getRetryCount())
                .humanLabel(domain.getHumanLabel())
                .humanReason(domain.getHumanReason())
                .createdAt(domain.getCreatedAt())
                .evaluatedAt(domain.getEvaluatedAt())
                .reviewedAt(domain.getReviewedAt())
                .retrievedInfo(domain.getRetrievedInfo())
                .isNew(isNew)
                .build();
    }

    public EvaluationCase toDomain(EvaluationCaseJpaEntity entity) {
        if (entity == null) return null;

        return EvaluationCase.reconstitute(
                entity.getEvaluationCaseId(),
                entity.getTargetTitle(),
                entity.getSourceTitle(),
                entity.getAiLabel(),
                entity.getAiConfidence(),
                entity.getEvaluationStatus(),
                entity.getRetryCount(),
                entity.getHumanLabel(),
                entity.getHumanReason(),
                entity.getCreatedAt(),
                entity.getEvaluatedAt(),
                entity.getReviewedAt(),
                entity.getRetrievedInfo()
        );
    }
}
