package evaluation.evaluationService.evaluation.adapter.out.persistence;

import evaluation.evaluationService.evaluation.domain.exception.EvaluationCaseNotFoundException;
import evaluation.evaluationService.evaluation.domain.model.EvaluationCase;
import evaluation.evaluationService.evaluation.domain.model.enums.EvaluationStatus;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class EvaluationCaseJpaAdapterTest {

    @Mock private EvaluationCaseSdjRepository repository;
    @Mock private EvaluationCaseMapper mapper;
    @InjectMocks private EvaluationCaseJpaAdapter adapter;

    @Test
    void idempotentInsertReturnsTrueOnSuccessfulFlush() {
        EvaluationCase domain = EvaluationCase.createPending("event-2", "Target", "Source");
        EvaluationCaseJpaEntity entity = mock(EvaluationCaseJpaEntity.class);

        when(mapper.toEntity(domain, true))
                .thenReturn(entity);

        assertTrue(adapter.saveIdempotent(domain));
        verify(repository).saveAndFlush(entity);
    }

    @Test
    void idempotentInsertReturnsFalseWhenDuplicateKeyViolatesIntegrity() {
        EvaluationCase domain = EvaluationCase.createPending("event-dup", "Target", "Source");
        EvaluationCaseJpaEntity entity = mock(EvaluationCaseJpaEntity.class);

        when(mapper.toEntity(domain, true)).thenReturn(entity);
        when(repository.saveAndFlush(entity)).thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertFalse(adapter.saveIdempotent(domain));
    }

    @Test
    void saveAndUpdatePassDistinctIsNewFlagsToMapper() {
        EvaluationCase domain = EvaluationCase.createPending("event-3", "Target", "Source");
        EvaluationCaseJpaEntity newEntity = mock(EvaluationCaseJpaEntity.class);
        EvaluationCaseJpaEntity existingEntity = mock(EvaluationCaseJpaEntity.class);

        when(mapper.toEntity(domain, true)).thenReturn(newEntity);
        when(mapper.toEntity(domain, false)).thenReturn(existingEntity);

        adapter.save(domain);
        adapter.update(domain);

        verify(repository).save(newEntity);
        verify(repository).save(existingEntity);
    }

    @Test
    void loadByIdReturnsMappedDomainWhenPresent() {
        EvaluationCaseJpaEntity entity = mock(EvaluationCaseJpaEntity.class);
        EvaluationCase domain = EvaluationCase.createPending("event-4", "Target", "Source");

        when(repository.findById("event-4")).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        assertSame(domain, adapter.loadById("event-4"));
    }

    @Test
    void loadByIdThrowsDomainNotFoundExceptionWhenAbsent() {
        when(repository.findById("missing"))
                .thenReturn(Optional.empty());

        assertThrows(EvaluationCaseNotFoundException.class, () -> adapter.loadById("missing"));
    }

    @Test
    void loadPendingEvaluationQueriesPendingAndFailedStatusesAndMapsResults() {
        EvaluationCaseJpaEntity entity = mock(EvaluationCaseJpaEntity.class);
        EvaluationCase domain = EvaluationCase.createPending("event-5", "Target", "Source");

        when(repository.findByEvaluationStatusIn(List.of(EvaluationStatus.PENDING, EvaluationStatus.FAILED)))
                .thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        assertEquals(List.of(domain), adapter.loadPendingEvaluation());
    }

    @Test
    void loadPendingReviewQueriesAiEvaluatedOrderedByConfidenceAscAndMapsResults() {
        EvaluationCaseJpaEntity entity = mock(EvaluationCaseJpaEntity.class);
        EvaluationCase domain = EvaluationCase.createPending("event-6", "Target", "Source");

        when(repository.findByEvaluationStatusOrderByAiConfidenceAsc(EvaluationStatus.AI_EVALUATED))
                .thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        assertEquals(List.of(domain), adapter.loadPendingReview());
    }
}
