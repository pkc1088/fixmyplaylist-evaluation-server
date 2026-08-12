package evaluation.evaluationService.evaluation.adapter.out.persistence;

import evaluation.evaluationService.evaluation.domain.model.ReferenceCase;
import evaluation.evaluationService.evaluation.domain.model.enums.EvaluationLabel;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "reference_cases")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReferenceCaseJpaEntity {

    @Id
    @Column(name = "reference_case_id")
    private String referenceCaseId;

    @Column(name = "target_title", nullable = false)
    private String targetTitle;

    @Column(name = "source_title", nullable = false)
    private String sourceTitle;

    @Enumerated(EnumType.STRING)
    @Column(name = "human_label")
    private EvaluationLabel humanLabel;

    @Column(name = "human_reason", columnDefinition = "TEXT")
    private String humanReason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    private ReferenceCaseJpaEntity(
            String referenceCaseId,
            String targetTitle,
            String sourceTitle,
            EvaluationLabel humanLabel,
            String humanReason,
            LocalDateTime createdAt
    ) {
        this.referenceCaseId = referenceCaseId;
        this.targetTitle = targetTitle;
        this.sourceTitle = sourceTitle;
        this.humanLabel = humanLabel;
        this.humanReason = humanReason;
        this.createdAt = createdAt;
    }

    public static ReferenceCaseJpaEntity from(ReferenceCase domain) {
        return ReferenceCaseJpaEntity.builder()
                .referenceCaseId(domain.getReferenceCaseId())
                .targetTitle(domain.getTargetTitle())
                .sourceTitle(domain.getSourceTitle())
                .humanLabel(domain.getHumanLabel())
                .humanReason(domain.getHumanReason())
                .createdAt(domain.getCreatedAt())
                .build();
    }

    public ReferenceCase toDomain() {
        return ReferenceCase.constitute(
                referenceCaseId, targetTitle, sourceTitle, humanLabel, humanReason, createdAt
        );
    }
}