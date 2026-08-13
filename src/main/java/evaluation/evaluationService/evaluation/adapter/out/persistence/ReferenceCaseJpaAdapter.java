package evaluation.evaluationService.evaluation.adapter.out.persistence;

import evaluation.evaluationService.evaluation.application.port.out.reference.QueryReferenceCasePort;
import evaluation.evaluationService.evaluation.application.port.out.reference.CommandReferenceCasePort;
import evaluation.evaluationService.evaluation.domain.model.ReferenceCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ReferenceCaseJpaAdapter implements QueryReferenceCasePort, CommandReferenceCasePort {

    private final ReferenceCaseSdjRepository repository;
    private final ReferenceCaseMapper mapper;


    @Override
    public void save(ReferenceCase referenceCase) {
        repository.save(mapper.toEntity(referenceCase, true));
    }

    @Override
    public void update(ReferenceCase referenceCase) {
        repository.save(mapper.toEntity(referenceCase, false));
    }

    @Override
    public void saveAll(List<ReferenceCase> referenceCases) {
        List<ReferenceCaseJpaEntity> entities = referenceCases.stream()
                .map(referenceCase -> mapper.toEntity(referenceCase, true))
                .toList();

        repository.saveAll(entities);
    }

    @Override
    public List<ReferenceCase> loadByIds(List<String> ids) {
        return repository.findAllById(ids).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<ReferenceCase> loadAll() {
        return repository.findAll().stream()
                .map(mapper::toDomain)
                .toList();
    }
}
