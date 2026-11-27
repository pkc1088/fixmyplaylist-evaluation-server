package evaluation.evaluationService.evaluation.domain.model;

import evaluation.evaluationService.evaluation.domain.model.enums.EvaluationLabel;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class ReferenceCaseTest {

    @Test
    void createsReferenceCaseWithReviewedFieldsAndStableId() {
        ReferenceCase reference = ReferenceCase.create("evaluation-1", "Target title", "Source title", EvaluationLabel.FAIL, "human reason");

        assertEquals("evaluation-1", reference.getReferenceCaseId());
        assertEquals("Target title", reference.getTargetTitle());
        assertEquals("Source title", reference.getSourceTitle());
        assertEquals(EvaluationLabel.FAIL, reference.getHumanLabel());
        assertEquals("human reason", reference.getHumanReason());
        assertNotNull(reference.getCreatedAt());
    }

    @Test
    void rejectsInvalidIdentityTitlesLabelAndReason() {
        assertThrows(IllegalArgumentException.class,
                () -> ReferenceCase.create(null, "Target", "Source", EvaluationLabel.SUCCESS, "reason"));
        assertThrows(IllegalArgumentException.class,
                () -> ReferenceCase.create(" ", "Target", "Source", EvaluationLabel.SUCCESS, "reason"));
        assertThrows(IllegalArgumentException.class,
                () -> ReferenceCase.create("a".repeat(101), "Target", "Source", EvaluationLabel.SUCCESS, "reason"));
        assertThrows(IllegalArgumentException.class,
                () -> ReferenceCase.create("id", null, "Source", EvaluationLabel.SUCCESS, "reason"));
        assertThrows(IllegalArgumentException.class,
                () -> ReferenceCase.create("id", " ", "Source", EvaluationLabel.SUCCESS, "reason"));
        assertThrows(IllegalArgumentException.class,
                () -> ReferenceCase.create("id", "Target", null, EvaluationLabel.SUCCESS, "reason"));
        assertThrows(IllegalArgumentException.class,
                () -> ReferenceCase.create("id", "Target", " ", EvaluationLabel.SUCCESS, "reason"));
        assertThrows(IllegalArgumentException.class,
                () -> ReferenceCase.create("id", "Target", "Source", null, "reason"));
        assertThrows(IllegalArgumentException.class,
                () -> ReferenceCase.create("id", "Target", "Source", EvaluationLabel.SUCCESS, null));
        assertThrows(IllegalArgumentException.class,
                () -> ReferenceCase.create("id", "Target", "Source", EvaluationLabel.SUCCESS, " "));
        assertThrows(IllegalArgumentException.class,
                () -> ReferenceCase.create("id", "Target", "Source", EvaluationLabel.SUCCESS, "r".repeat(4001)));
    }

    @Test
    void acceptsBoundaryReasonLengthAndTruncatesTitlesAtStorageLimit() {
        String longTitle = "x".repeat(300);
        String maxReason = "r".repeat(4000);

        ReferenceCase reference = ReferenceCase.create("a".repeat(100), longTitle, longTitle, EvaluationLabel.SUCCESS, maxReason);

        assertEquals(255, reference.getTargetTitle().length());
        assertEquals(255, reference.getSourceTitle().length());
        assertEquals(maxReason, reference.getHumanReason());
    }

    @Test
    void reconstitutePreservesCreatedAtOrDefaultsWhenNull() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 2, 3, 4, 5, 6);

        ReferenceCase withTimestamp = ReferenceCase.reconstitute(
                "ref-1", "Target", "Source", EvaluationLabel.SUCCESS, "reason", createdAt);
        assertEquals(createdAt, withTimestamp.getCreatedAt());

        ReferenceCase withoutTimestamp = ReferenceCase.reconstitute(
                "ref-2", "Target", "Source", EvaluationLabel.FAIL, "reason", null);
        assertNotNull(withoutTimestamp.getCreatedAt());
    }
}
