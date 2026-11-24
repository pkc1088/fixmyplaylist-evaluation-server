package evaluation.evaluationService.evaluation.adapter.out.persistence;

import evaluation.evaluationService.evaluation.domain.model.ReferenceCase;
import evaluation.evaluationService.evaluation.domain.model.enums.EvaluationLabel;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class ReferenceCaseMapperTest {

    private final ReferenceCaseMapper mapper = new ReferenceCaseMapper();

    @Test
    void mapsDomainToEntityAndBackWithInsertFlagAndHumanData() {
        ReferenceCase domain = ReferenceCase.create("reference-1", "Target", "Source", EvaluationLabel.FAIL, "Human reason");

        ReferenceCaseJpaEntity entity = mapper.toEntity(domain, true);
        ReferenceCase mappedBack = mapper.toDomain(entity);

        assertTrue(entity.isNew());
        assertEquals("reference-1", mappedBack.getReferenceCaseId());
        assertEquals("Target", mappedBack.getTargetTitle());
        assertEquals("Source", mappedBack.getSourceTitle());
        assertEquals(EvaluationLabel.FAIL, mappedBack.getHumanLabel());
        assertEquals("Human reason", mappedBack.getHumanReason());
        assertFalse(mapper.toEntity(domain, false).isNew());
    }

    @Test
    void nullInputMapsToNull() {
        assertNull(mapper.toEntity(null, false));
        assertNull(mapper.toDomain(null));
    }
}
