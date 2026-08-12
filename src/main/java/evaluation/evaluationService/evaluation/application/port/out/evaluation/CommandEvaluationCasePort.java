package evaluation.evaluationService.evaluation.application.port.out.evaluation;

import evaluation.evaluationService.evaluation.domain.model.EvaluationCase;

public interface CommandEvaluationCasePort {

    void save(EvaluationCase evaluationCase);

    void update(EvaluationCase evaluationCase);

    boolean existsById(String recoveryId);
}
