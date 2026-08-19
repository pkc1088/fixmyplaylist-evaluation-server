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

    private static final int MAX_REFERENCE_CASE_ID_LENGTH = 100;
    private static final int MAX_TARGET_TITLE_LENGTH = 255;
    private static final int MAX_SOURCE_TITLE_LENGTH = 255;

    public static final int RAG_TOP_K = 5;


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
        this.targetTitle = truncate(targetTitle, MAX_TARGET_TITLE_LENGTH);
        this.sourceTitle = truncate(sourceTitle, MAX_SOURCE_TITLE_LENGTH);
        this.humanLabel = humanLabel;
        this.humanReason = humanReason;
        this.createdAt = (createdAt != null) ? createdAt : LocalDateTime.now();

        validate();
    }

    private static String truncate(String value, int maxLength) {
        if (value == null) return null;
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }

    private void validate() {
        if (referenceCaseId == null || referenceCaseId.isBlank() || referenceCaseId.length() > MAX_REFERENCE_CASE_ID_LENGTH) {
            throw new IllegalArgumentException("ReferenceCaseId must not be null");
        }
        if (targetTitle == null || targetTitle.isBlank() || targetTitle.length() > MAX_TARGET_TITLE_LENGTH) {
            throw new IllegalArgumentException("TargetTitle must not be null");
        }
        if (sourceTitle == null || sourceTitle.isBlank() || sourceTitle.length() > MAX_SOURCE_TITLE_LENGTH) {
            throw new IllegalArgumentException("SourceTitle must not be null");
        }
        if (this.humanLabel == null) {
            throw new IllegalArgumentException("HumanLabel cannot be null");
        }
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
