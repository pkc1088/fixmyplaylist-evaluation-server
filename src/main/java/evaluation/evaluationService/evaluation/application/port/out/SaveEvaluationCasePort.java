package evaluation.evaluationService.evaluation.application.port.out;

import evaluation.evaluationService.evaluation.domain.model.EvaluationCase;

public interface SaveEvaluationCasePort {

    void save(EvaluationCase evaluationCase);

    boolean existsById(String recoveryId);
}
