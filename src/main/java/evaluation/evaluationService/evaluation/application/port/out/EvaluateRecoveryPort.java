package evaluation.evaluationService.evaluation.application.port.out;

import evaluation.evaluationService.evaluation.domain.model.vo.EvaluationResult;
import evaluation.evaluationService.evaluation.domain.model.vo.RecoveryCase;
import evaluation.evaluationService.evaluation.domain.model.vo.RetrievedCase;

import java.util.List;

public interface EvaluateRecoveryPort {

    EvaluationResult evaluateZeroShot(RecoveryCase testCase);

    EvaluationResult evaluateWithRag(RecoveryCase testCase, List<RetrievedCase> similarCases);
}
