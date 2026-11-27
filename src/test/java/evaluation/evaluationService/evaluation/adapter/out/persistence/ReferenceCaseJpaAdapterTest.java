package evaluation.evaluationService.evaluation.adapter.out.persistence;

import evaluation.evaluationService.evaluation.domain.model.ReferenceCase;
import evaluation.evaluationService.evaluation.domain.model.enums.EvaluationLabel;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class ReferenceCaseJpaAdapterTest {

    @Mock private ReferenceCaseSdjRepository repository;
    @Mock private ReferenceCaseMapper mapper;
    @InjectMocks private ReferenceCaseJpaAdapter adapter;
    @Captor private ArgumentCaptor<List<ReferenceCaseJpaEntity>> entitiesCaptor;

    @Test
    void saveAllMarksNewAndExistingIdsForUpsert() {
        ReferenceCase newCase = reference("new-id");
        ReferenceCase existingCase = reference("existing-id");
        ReferenceCaseJpaEntity existingEntity = entity("existing-id");
        ReferenceCaseJpaEntity newEntity = entity("new-id");
        ReferenceCaseJpaEntity updatedEntity = entity("existing-id");

        when(repository.findAllById(List.of("new-id", "existing-id"))).thenReturn(List.of(existingEntity));
        when(mapper.toEntity(newCase, true)).thenReturn(newEntity);
        when(mapper.toEntity(existingCase, false)).thenReturn(updatedEntity);

        adapter.saveAll(List.of(newCase, existingCase));

        verify(repository).saveAll(entitiesCaptor.capture());
        assertEquals(List.of(newEntity, updatedEntity), entitiesCaptor.getValue());
    }

    @Test
    void saveAllDoesNothingForNullOrEmptyInput() {
        adapter.saveAll(null);
        adapter.saveAll(List.of());

        verifyNoInteractions(repository, mapper);
    }

    @Test
    void saveAndUpdatePassDistinctIsNewFlagsToMapper() {
        ReferenceCase domain = reference("ref-1");
        ReferenceCaseJpaEntity newEntity = entity("ref-1");
        ReferenceCaseJpaEntity updatedEntity = entity("ref-1");

        when(mapper.toEntity(domain, true)).thenReturn(newEntity);
        when(mapper.toEntity(domain, false)).thenReturn(updatedEntity);

        adapter.save(domain);
        adapter.update(domain);

        verify(repository).save(newEntity);
        verify(repository).save(updatedEntity);
    }

    @Test
    void loadByIdsAndLoadAllMapEntitiesToDomain() {
        ReferenceCaseJpaEntity entity = entity("ref-2");
        ReferenceCase domain = reference("ref-2");

        when(repository.findAllById(List.of("ref-2"))).thenReturn(List.of(entity));
        when(repository.findAll()).thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        assertEquals(List.of(domain), adapter.loadByIds(List.of("ref-2")));
        assertEquals(List.of(domain), adapter.loadAll());
    }

    private ReferenceCase reference(String id) {
        return ReferenceCase.create(id, "Target", "Source", EvaluationLabel.SUCCESS, "reason");
    }

    private ReferenceCaseJpaEntity entity(String id) {
        return ReferenceCaseJpaEntity.builder().referenceCaseId(id).build();
    }
}
