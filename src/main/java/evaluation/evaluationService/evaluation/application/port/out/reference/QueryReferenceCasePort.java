package evaluation.evaluationService.evaluation.application.port.out.reference;

import evaluation.evaluationService.evaluation.domain.model.ReferenceCase;

import java.util.List;

public interface QueryReferenceCasePort {

    List<ReferenceCase> loadByIds(List<String> ids);
}
