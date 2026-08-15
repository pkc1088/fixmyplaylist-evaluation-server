package evaluation.evaluationService.evaluation.domain.model;

import evaluation.evaluationService.evaluation.domain.model.enums.EvaluationLabel;
import evaluation.evaluationService.evaluation.domain.model.enums.EvaluationStatus;
import evaluation.evaluationService.evaluation.domain.model.vo.ReferenceTrace;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
public class EvaluationCase {

    private final String evaluationCaseId;

    private final String targetTitle;

    private final String sourceTitle;

    private EvaluationLabel aiLabel;

    private Double aiConfidence;

    private EvaluationStatus evaluationStatus;

    private EvaluationLabel humanLabel;

    private String humanReason;

    private LocalDateTime createdAt;

    private LocalDateTime evaluatedAt;

    private LocalDateTime reviewedAt;

    private List<ReferenceTrace> retrievedInfo;


    @Builder(access = AccessLevel.PRIVATE)
    private EvaluationCase(
            String evaluationCaseId,
            String targetTitle,
            String sourceTitle,
            EvaluationLabel aiLabel,
            Double aiConfidence,
            EvaluationStatus evaluationStatus,
            EvaluationLabel humanLabel,
            String humanReason,
            LocalDateTime createdAt,
            LocalDateTime evaluatedAt,
            LocalDateTime reviewedAt,
            List<ReferenceTrace> retrievedInfo
    ) {
        this.evaluationCaseId = evaluationCaseId;
        this.targetTitle = targetTitle;
        this.sourceTitle = sourceTitle;
        this.aiLabel = aiLabel;
        this.aiConfidence = aiConfidence;
        this.evaluationStatus = evaluationStatus;
        this.humanLabel = humanLabel;
        this.humanReason = humanReason;
        this.createdAt = (createdAt != null) ? createdAt : LocalDateTime.now();
        this.evaluatedAt = evaluatedAt;
        this.reviewedAt = reviewedAt;
        this.retrievedInfo = (retrievedInfo != null) ? retrievedInfo : new ArrayList<>();

        validate();
    }

    private void validate() {
        if (targetTitle == null || sourceTitle == null) {
            throw new IllegalArgumentException("Title must not be null");
        }
    }

    public static EvaluationCase createPending(
            String evaluationCaseId,
            String targetTitle,
            String sourceTitle
    ) {
        return EvaluationCase.builder()
                .evaluationCaseId(evaluationCaseId)
                .targetTitle(targetTitle)
                .sourceTitle(sourceTitle)
                .evaluationStatus(EvaluationStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .retrievedInfo(new ArrayList<>())
                .build();
    }

    public EvaluationCase applyAiEvaluation(
            EvaluationLabel aiLabel,
            Double aiConfidence,
            List<ReferenceTrace> retrievedReferenceIds
    ) {
        return EvaluationCase.builder()
                .evaluationCaseId(this.evaluationCaseId)
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
                .retrievedInfo(retrievedReferenceIds)
                .build();
    }

    public EvaluationCase applyHumanReview(
            EvaluationLabel humanLabel,
            String humanReason
    ) {
        return EvaluationCase.builder()
                .evaluationCaseId(this.evaluationCaseId)
                .targetTitle(this.targetTitle)
                .sourceTitle(this.sourceTitle)
                .aiLabel(this.aiLabel)
                .aiConfidence(this.aiConfidence)
                .evaluationStatus(EvaluationStatus.HUMAN_REVIEWED)
                .humanLabel(humanLabel)
                .humanReason(humanReason)
                .createdAt(this.createdAt)
                .evaluatedAt(this.evaluatedAt)
                .reviewedAt(LocalDateTime.now())
                .retrievedInfo(retrievedInfo)
                .build();
    }

    public static EvaluationCase reconstitute(
            String evaluationCaseId,
            String targetTitle,
            String sourceTitle,
            EvaluationLabel aiLabel,
            Double aiConfidence,
            EvaluationStatus evaluationStatus,
            EvaluationLabel humanLabel,
            String humanReason,
            LocalDateTime createdAt,
            LocalDateTime evaluatedAt,
            LocalDateTime reviewedAt,
            List<ReferenceTrace> retrievedReferenceIds
    ) {
        return EvaluationCase.builder()
                .evaluationCaseId(evaluationCaseId)
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
                .retrievedInfo(retrievedReferenceIds)
                .build();
    }
}