package evaluation.evaluationService.evaluation.application.port.out;

import evaluation.evaluationService.evaluation.domain.model.ReferenceCase;

import java.util.List;

public interface QueryReferenceCasePort {

    List<ReferenceCase> loadByIds(List<String> ids);

    List<ReferenceCase> loadAll();
}
