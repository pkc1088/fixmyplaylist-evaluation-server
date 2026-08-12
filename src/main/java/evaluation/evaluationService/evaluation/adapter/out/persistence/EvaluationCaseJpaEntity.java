package evaluation.evaluationService.evaluation.adapter.out.persistence;

import evaluation.evaluationService.evaluation.domain.model.enums.EvaluationLabel;
import evaluation.evaluationService.evaluation.domain.model.enums.EvaluationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.domain.Persistable;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "evaluation_cases")
public class EvaluationCaseJpaEntity implements Persistable<String> {

    @Id
    @Column(length = 100) // = Kafka 이벤트 ID, Inbox 패턴 de dup key
    private String evaluationCaseId;

    @Column(nullable = false, length = 255)
    private String targetTitle;

    @Column(nullable = false, length = 255)
    private String sourceTitle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true, length = 20)
    private EvaluationLabel aiLabel;

    @Column(nullable = true)
    private Double aiConfidence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EvaluationStatus evaluationStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true, length = 20)
    private EvaluationLabel humanLabel;

    @Column(nullable = true, columnDefinition = "TEXT")
    private String humanReason;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = true)
    private LocalDateTime evaluatedAt;

    @Column(nullable = true)
    private LocalDateTime reviewedAt;

    @Transient
    @Builder.Default
    private boolean isNew = false;

    @Override
    public String getId() {
        return this.evaluationCaseId;
    }

    @Override
    public boolean isNew() {
        return this.isNew;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

//    @Builder
//    private EvaluationCaseJpaEntity(
//            String evaluationCaseId,
//            String targetTitle,
//            String sourceTitle,
//            EvaluationLabel aiLabel,
//            Double aiConfidence,
//            EvaluationStatus evaluationStatus,
//            EvaluationLabel humanLabel,
//            String humanReason,
//            LocalDateTime createdAt,
//            LocalDateTime evaluatedAt,
//            LocalDateTime reviewedAt
//    ) {
//        this.evaluationCaseId = evaluationCaseId;
//        this.targetTitle = targetTitle;
//        this.sourceTitle = sourceTitle;
//        this.aiLabel = aiLabel;
//        this.aiConfidence = aiConfidence;
//        this.evaluationStatus = evaluationStatus;
//        this.humanLabel = humanLabel;
//        this.humanReason = humanReason;
//        this.createdAt = createdAt;
//        this.evaluatedAt = evaluatedAt;
//        this.reviewedAt = reviewedAt;
//    }
//    public static EvaluationCaseJpaEntity from(EvaluationCase domain) {
//        return EvaluationCaseJpaEntity.builder()
//                .evaluationCaseId(domain.getEvaluationCaseId())
//                .targetTitle(domain.getTargetTitle())
//                .sourceTitle(domain.getSourceTitle())
//                .aiLabel(domain.getAiLabel())
//                .aiConfidence(domain.getAiConfidence())
//                .evaluationStatus(domain.getEvaluationStatus())
//                .humanLabel(domain.getHumanLabel())
//                .humanReason(domain.getHumanReason())
//                .createdAt(domain.getCreatedAt())
//                .evaluatedAt(domain.getEvaluatedAt())
//                .reviewedAt(domain.getReviewedAt())
//                .build();
//    }
//
//    public EvaluationCase toDomain() {
//        return EvaluationCase.reconstitute(
//                evaluationCaseId, targetTitle, sourceTitle, aiLabel, aiConfidence, evaluationStatus,
//                humanLabel, humanReason, createdAt, evaluatedAt, reviewedAt
//        );
//    }
}