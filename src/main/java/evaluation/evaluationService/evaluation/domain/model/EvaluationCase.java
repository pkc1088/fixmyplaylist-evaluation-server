package evaluation.evaluationService.evaluation.domain.model;

import evaluation.evaluationService.evaluation.domain.model.enums.EvaluationLabel;
import evaluation.evaluationService.evaluation.domain.model.enums.EvaluationStatus;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class EvaluationCase {

    private String recoveryId;

    private String targetTitle;

    private String sourceTitle;

    private EvaluationLabel aiLabel;

    private Double aiConfidence;

    private EvaluationStatus evaluationStatus;

    private EvaluationLabel humanLabel; // nullable

    private String humanReason; // nullable

    private LocalDateTime createdAt;

    private LocalDateTime evaluatedAt;

    private LocalDateTime reviewedAt;


    @Builder(access = AccessLevel.PRIVATE)
    private EvaluationCase(
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
        this.createdAt = (createdAt != null) ? createdAt : LocalDateTime.now();
        this.evaluatedAt = (evaluatedAt != null) ? evaluatedAt : LocalDateTime.now();
        this.reviewedAt = reviewedAt;

        validate();
    }

    private void validate() {
        if (targetTitle == null || sourceTitle == null) {
            throw new IllegalArgumentException("Title must not be null");
        }
    }

    public static EvaluationCase createPending(String recoveryId, String targetTitle, String sourceTitle) {
        return EvaluationCase.builder()
                .recoveryId(recoveryId)
                .targetTitle(targetTitle)
                .sourceTitle(sourceTitle)
                .evaluationStatus(EvaluationStatus.PENDING)
                .build();
    }

    public EvaluationCase applyAiEvaluation(EvaluationLabel aiLabel, Double aiConfidence) {
        return EvaluationCase.builder()
                .recoveryId(this.recoveryId)
                .targetTitle(this.targetTitle)
                .sourceTitle(this.sourceTitle)
                .aiLabel(aiLabel)
                .aiConfidence(aiConfidence)
                .evaluationStatus(EvaluationStatus.AI_EVALUATED)
                .humanLabel(this.humanLabel)
                .humanReason(this.humanReason)
                .createdAt(this.createdAt)
                .evaluatedAt(LocalDateTime.now())
                .reviewedAt(this.reviewedAt)
                .build();
    }

    public static EvaluationCase constitute(
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
        return EvaluationCase.builder()
                .recoveryId(recoveryId)
                .targetTitle(targetTitle)
                .sourceTitle(sourceTitle)
                .aiLabel(aiLabel)
                .aiConfidence(aiConfidence)
                .evaluationStatus(evaluationStatus)
                .humanLabel(humanLabel)
                .humanReason(humanReason)
                .createdAt(createdAt)
                .evaluatedAt(evaluatedAt)
                .reviewedAt(reviewedAt)
                .build();
    }
}