package evaluation.evaluationService.evaluation.application.port.out;

import evaluation.evaluationService.evaluation.domain.model.vo.RecoveryCase;
import evaluation.evaluationService.evaluation.domain.model.vo.RetrievedCase;

import java.util.List;

public interface RetrieveReferenceCasePort {

    void index(List<RecoveryCase> cases);

    List<RetrievedCase> retrieve(RecoveryCase testCase, int topK);
}
