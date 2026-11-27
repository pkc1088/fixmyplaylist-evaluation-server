package evaluation.evaluationService.evaluation.adapter.out.persistence;

import evaluation.evaluationService.evaluation.domain.model.EvaluationCase;
import evaluation.evaluationService.evaluation.domain.model.enums.EvaluationLabel;
import evaluation.evaluationService.evaluation.domain.model.enums.EvaluationStatus;
import evaluation.evaluationService.evaluation.domain.model.vo.ReferenceTrace;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class EvaluationCaseMapperTest {

    private final EvaluationCaseMapper mapper = new EvaluationCaseMapper();

    @Test
    void mapsDomainToNewEntityAndBackWithoutLosingEvaluationOrTrace() {
        EvaluationCase domain = EvaluationCase.createPending("event-1", "Target", "Source");
        domain.handleFailure();
        domain.applyAiEvaluation(EvaluationLabel.FAIL, 0.61, List.of(new ReferenceTrace("reference-1", 0.88)));
        domain.applyHumanReview(EvaluationLabel.SUCCESS, "Human verified");

        EvaluationCaseJpaEntity entity = mapper.toEntity(domain, true);
        EvaluationCase mappedBack = mapper.toDomain(entity);

        assertTrue(entity.isNew());
        assertEquals("event-1", entity.getId());
        assertEquals(domain.getEvaluationCaseId(), mappedBack.getEvaluationCaseId());
        assertEquals(domain.getTargetTitle(), mappedBack.getTargetTitle());
        assertEquals(domain.getSourceTitle(), mappedBack.getSourceTitle());
        assertEquals(domain.getEvaluationStatus(), mappedBack.getEvaluationStatus());
        assertEquals(domain.getRetryCount(), mappedBack.getRetryCount());
        assertEquals(domain.getAiLabel(), mappedBack.getAiLabel());
        assertEquals(domain.getAiConfidence(), mappedBack.getAiConfidence());
        assertEquals(domain.getRetrievedInfo(), mappedBack.getRetrievedInfo());
        assertEquals(domain.getHumanLabel(), mappedBack.getHumanLabel());
        assertEquals(domain.getHumanReason(), mappedBack.getHumanReason());
        assertEquals(domain.getCreatedAt(), mappedBack.getCreatedAt());
        assertEquals(domain.getEvaluatedAt(), mappedBack.getEvaluatedAt());
        assertEquals(domain.getReviewedAt(), mappedBack.getReviewedAt());
        assertFalse(mapper.toEntity(domain, false).isNew());

        entity.markNotNew();
        assertFalse(entity.isNew());
    }

    @Test
    void mapsPendingCaseWithNullableEvaluationFields() {
        EvaluationCase pending = EvaluationCase.createPending("event-pending", "Target", "Source");

        EvaluationCaseJpaEntity entity = mapper.toEntity(pending, true);
        EvaluationCase mappedBack = mapper.toDomain(entity);

        assertEquals(EvaluationStatus.PENDING, mappedBack.getEvaluationStatus());
        assertEquals(0, mappedBack.getRetryCount());
        assertNull(mappedBack.getAiLabel());
        assertNull(mappedBack.getAiConfidence());
        assertNull(mappedBack.getHumanLabel());
        assertNull(mappedBack.getHumanReason());
        assertNull(mappedBack.getEvaluatedAt());
        assertNull(mappedBack.getReviewedAt());
        assertEquals(pending.getCreatedAt(), mappedBack.getCreatedAt());
        assertTrue(mappedBack.getRetrievedInfo().isEmpty());
    }

    @Test
    void nullInputMapsToNull() {
        assertNull(mapper.toEntity(null, true));
        assertNull(mapper.toDomain(null));
    }
}
