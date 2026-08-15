package evaluation.evaluationService.evaluation.application.port.out;

import evaluation.evaluationService.evaluation.application.port.out.dto.VectorSearchResult;
import evaluation.evaluationService.evaluation.domain.model.EvaluationCase;
import evaluation.evaluationService.evaluation.domain.model.ReferenceCase;

import java.util.List;

public interface RetrieveReferenceCasePort {

    void index(List<ReferenceCase> cases);

    List<VectorSearchResult> retrieveIds(EvaluationCase testCase, int topK);
}
