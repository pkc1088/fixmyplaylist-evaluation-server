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


    // [셋업 및 관리자 단계] ReferenceCase: Upsert & Persistable & jdbc.batch_size 최적화
    @Override
    public void saveAll(List<ReferenceCase> referenceCases) {
        if (referenceCases == null || referenceCases.isEmpty()) return;

        List<String> ids = referenceCases.stream()
                .map(ReferenceCase::getReferenceCaseId)
                .toList();

        List<String> existingIds = repository.findAllById(ids).stream()
                .map(ReferenceCaseJpaEntity::getReferenceCaseId)
                .toList();

        List<ReferenceCaseJpaEntity> entities = referenceCases.stream()
                .map(rc -> {
                    boolean isNew = !existingIds.contains(rc.getReferenceCaseId());
                    return mapper.toEntity(rc, isNew);
                })
                .toList();

        repository.saveAll(entities);
    }

    @Override
    public void save(ReferenceCase referenceCase) {
        repository.save(mapper.toEntity(referenceCase, true));
    }

    @Override
    public void update(ReferenceCase referenceCase) {
        repository.save(mapper.toEntity(referenceCase, false));
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
