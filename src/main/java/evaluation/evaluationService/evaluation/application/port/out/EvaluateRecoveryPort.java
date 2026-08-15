package evaluation.evaluationService.evaluation.application.port.out;

import evaluation.evaluationService.evaluation.domain.model.EvaluationCase;
import evaluation.evaluationService.evaluation.application.port.out.dto.EvaluationResult;
import evaluation.evaluationService.evaluation.domain.model.vo.RetrievedCase;

import java.util.List;

public interface EvaluateRecoveryPort {

    EvaluationResult evaluateWithRag(EvaluationCase testCase, List<RetrievedCase> similarCases);
}
