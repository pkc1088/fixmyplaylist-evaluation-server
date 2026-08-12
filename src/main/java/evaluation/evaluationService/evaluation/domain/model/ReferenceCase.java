package evaluation.evaluationService.evaluation.domain.model;

import evaluation.evaluationService.evaluation.domain.model.enums.EvaluationLabel;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ReferenceCase {

    private final String referenceCaseId;

    private final String targetTitle;

    private final String sourceTitle;

    private EvaluationLabel humanLabel;

    private String humanReason;

    private LocalDateTime createdAt;


    @Builder(access = AccessLevel.PRIVATE)
    private ReferenceCase(
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
        this.createdAt = (createdAt != null) ? createdAt : LocalDateTime.now();
    }

    private void validate() {
    }

    public static ReferenceCase create(
            String referenceCaseId,
            String targetTitle,
            String sourceTitle,
            EvaluationLabel humanLabel,
            String humanReason
    ) {
        return ReferenceCase.builder()
                .referenceCaseId(referenceCaseId)
                .targetTitle(targetTitle)
                .sourceTitle(sourceTitle)
                .humanLabel(humanLabel)
                .humanReason(humanReason)
                .build();
    }

    public static ReferenceCase reconstitute(
            String referenceCaseId,
            String targetTitle,
            String sourceTitle,
            EvaluationLabel humanLabel,
            String humanReason,
            LocalDateTime createdAt
    ) {
        return ReferenceCase.builder()
                .referenceCaseId(referenceCaseId)
                .targetTitle(targetTitle)
                .sourceTitle(sourceTitle)
                .humanLabel(humanLabel)
                .humanReason(humanReason)
                .createdAt(createdAt)
                .build();
    }
}
