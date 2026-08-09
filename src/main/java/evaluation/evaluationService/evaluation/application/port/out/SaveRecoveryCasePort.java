package evaluation.evaluationService.evaluation.application.port.out;

import evaluation.evaluationService.evaluation.domain.model.vo.EvaluationOutput;

import java.io.IOException;
import java.util.List;

public interface SaveRecoveryCasePort {

    void exportResults(List<EvaluationOutput> results) throws IOException;
}
