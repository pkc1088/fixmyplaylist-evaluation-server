package evaluation.evaluationService.evaluation.application.port.out;

import evaluation.evaluationService.evaluation.domain.model.ReferenceCase;

import java.util.List;

public interface SaveReferenceCasePort {

    void save(ReferenceCase referenceCase);

    void saveAll(List<ReferenceCase> referenceCases);
}
