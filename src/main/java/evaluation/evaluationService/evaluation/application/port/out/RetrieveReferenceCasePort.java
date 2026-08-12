package evaluation.evaluationService.evaluation.application.port.out;

import evaluation.evaluationService.evaluation.domain.model.EvaluationCase;
import evaluation.evaluationService.evaluation.domain.model.ReferenceCase;
import evaluation.evaluationService.evaluation.domain.model.vo.RetrievedCase;

import java.util.List;

public interface RetrieveReferenceCasePort {

    void index(List<ReferenceCase> cases);

    List<RetrievedCase> retrieve(EvaluationCase testCase, int topK);
}
