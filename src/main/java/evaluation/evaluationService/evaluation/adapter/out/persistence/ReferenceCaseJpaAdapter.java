package evaluation.evaluationService.evaluation.adapter.out.persistence;

import evaluation.evaluationService.evaluation.application.port.out.LoadReferenceCasePort;
import evaluation.evaluationService.evaluation.application.port.out.SaveReferenceCasePort;
import evaluation.evaluationService.evaluation.domain.model.ReferenceCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ReferenceCaseJpaAdapter implements LoadReferenceCasePort, SaveReferenceCasePort {

    private final ReferenceCaseSdjRepository repository;


    @Override
    public List<ReferenceCase> loadByIds(List<String> ids) {
        return repository.findAllById(ids).stream()
                .map(ReferenceCaseJpaEntity::toDomain)
                .toList();
    }

    @Override
    public void save(ReferenceCase referenceCase) {
        repository.save(ReferenceCaseJpaEntity.from(referenceCase));
    }

    @Override
    public void saveAll(List<ReferenceCase> referenceCases) {
        repository.saveAll(referenceCases.stream().map(ReferenceCaseJpaEntity::from).toList());
    }
}
