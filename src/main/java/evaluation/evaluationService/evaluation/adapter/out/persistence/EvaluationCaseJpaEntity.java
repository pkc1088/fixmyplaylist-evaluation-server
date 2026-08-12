package evaluation.evaluationService.evaluation.adapter.out.persistence;

import evaluation.evaluationService.evaluation.domain.model.EvaluationCase;
import evaluation.evaluationService.evaluation.domain.model.enums.EvaluationLabel;
import evaluation.evaluationService.evaluation.domain.model.enums.EvaluationStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "evaluation_cases")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EvaluationCaseJpaEntity {

    @Id
    @Column(name = "recovery_id") // = Kafka 이벤트 ID, Inbox 패턴 dedup key
    private String recoveryId;

    @Column(name = "target_title", nullable = false)
    private String targetTitle;

    @Column(name = "source_title", nullable = false)
    private String sourceTitle;

    @Enumerated(EnumType.STRING)
    @Column(name = "ai_label")
    private EvaluationLabel aiLabel;

    @Column(name = "ai_confidence")
    private Double aiConfidence;

    @Enumerated(EnumType.STRING)
    @Column(name = "evaluation_status", nullable = false)
    private EvaluationStatus evaluationStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "human_label")
    private EvaluationLabel humanLabel;

    @Column(name = "human_reason", columnDefinition = "TEXT")
    private String humanReason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "evaluated_at")
    private LocalDateTime evaluatedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Builder
    private EvaluationCaseJpaEntity(
            String recoveryId,
            String targetTitle,
            String sourceTitle,
            EvaluationLabel aiLabel,
            Double aiConfidence,
            EvaluationStatus evaluationStatus,
            EvaluationLabel humanLabel,
            String humanReason,
            LocalDateTime createdAt,
            LocalDateTime evaluatedAt,
            LocalDateTime reviewedAt
    ) {
        this.recoveryId = recoveryId;
        this.targetTitle = targetTitle;
        this.sourceTitle = sourceTitle;
        this.aiLabel = aiLabel;
        this.aiConfidence = aiConfidence;
        this.evaluationStatus = evaluationStatus;
        this.humanLabel = humanLabel;
        this.humanReason = humanReason;
        this.createdAt = createdAt;
        this.evaluatedAt = evaluatedAt;
        this.reviewedAt = reviewedAt;
    }

    public static EvaluationCaseJpaEntity from(EvaluationCase domain) {
        return EvaluationCaseJpaEntity.builder()
                .recoveryId(domain.getRecoveryId())
                .targetTitle(domain.getTargetTitle())
                .sourceTitle(domain.getSourceTitle())
                .aiLabel(domain.getAiLabel())
                .aiConfidence(domain.getAiConfidence())
                .evaluationStatus(domain.getEvaluationStatus())
                .humanLabel(domain.getHumanLabel())
                .humanReason(domain.getHumanReason())
                .createdAt(domain.getCreatedAt())
                .evaluatedAt(domain.getEvaluatedAt())
                .reviewedAt(domain.getReviewedAt())
                .build();
    }

    public EvaluationCase toDomain() {
        return EvaluationCase.constitute(
                recoveryId, targetTitle, sourceTitle, aiLabel, aiConfidence, evaluationStatus,
                humanLabel, humanReason, createdAt, evaluatedAt, reviewedAt
        );
    }
}