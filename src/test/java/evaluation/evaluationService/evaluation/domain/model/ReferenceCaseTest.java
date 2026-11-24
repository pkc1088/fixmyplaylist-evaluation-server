package evaluation.evaluationService.evaluation.domain.model;

import evaluation.evaluationService.evaluation.domain.model.enums.EvaluationLabel;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

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
    void rejectsMissingIdentityTitlesAndLabel() {
        assertThrows(IllegalArgumentException.class,
                () -> ReferenceCase.create(" ", "Target", "Source", EvaluationLabel.SUCCESS, "reason"));
        assertThrows(IllegalArgumentException.class,
                () -> ReferenceCase.create("id", " ", "Source", EvaluationLabel.SUCCESS, "reason"));
        assertThrows(IllegalArgumentException.class,
                () -> ReferenceCase.create("id", "Target", null, EvaluationLabel.SUCCESS, "reason"));
        assertThrows(IllegalArgumentException.class,
                () -> ReferenceCase.create("id", "Target", "Source", null, "reason"));
    }

    @Test
    void truncatesTitlesAtStorageLimit() {
        String longTitle = "x".repeat(300);

        ReferenceCase reference = ReferenceCase.create("id", longTitle, longTitle, EvaluationLabel.SUCCESS, "reason");

        assertEquals(255, reference.getTargetTitle().length());
        assertEquals(255, reference.getSourceTitle().length());
    }
}
