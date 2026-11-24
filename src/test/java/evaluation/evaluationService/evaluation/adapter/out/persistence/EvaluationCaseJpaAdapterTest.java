package evaluation.evaluationService.evaluation.adapter.out.persistence;

import evaluation.evaluationService.evaluation.domain.exception.EvaluationCaseNotFoundException;
import evaluation.evaluationService.evaluation.domain.model.EvaluationCase;
import evaluation.evaluationService.evaluation.domain.model.enums.EvaluationStatus;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
    void loadByIdThrowsDomainNotFoundExceptionWhenAbsent() {
        when(repository.findById("missing"))
                .thenReturn(Optional.empty());

        assertThrows(EvaluationCaseNotFoundException.class, () -> adapter.loadById("missing"));
    }
}
