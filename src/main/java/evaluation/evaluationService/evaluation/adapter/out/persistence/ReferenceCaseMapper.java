package evaluation.evaluationService.evaluation.adapter.out.persistence;

import evaluation.evaluationService.evaluation.domain.model.ReferenceCase;
import org.springframework.stereotype.Component;

@Component
public class ReferenceCaseMapper {

    public ReferenceCaseJpaEntity toEntity(ReferenceCase domain, boolean isNew) {
        if (domain == null) return null;

        return ReferenceCaseJpaEntity.builder()
                .referenceCaseId(domain.getReferenceCaseId())
                .targetTitle(domain.getTargetTitle())
                .sourceTitle(domain.getSourceTitle())
                .humanLabel(domain.getHumanLabel())
                .humanReason(domain.getHumanReason())
                .createdAt(domain.getCreatedAt())
                .isNew(isNew)
                .build();
    }

    public ReferenceCase toDomain(ReferenceCaseJpaEntity entity) {
        if (entity == null) return null;

        return ReferenceCase.reconstitute(
                entity.getReferenceCaseId(),
                entity.getTargetTitle(),
                entity.getSourceTitle(),
                entity.getHumanLabel(),
                entity.getHumanReason(),
                entity.getCreatedAt()
        );
    }
}
