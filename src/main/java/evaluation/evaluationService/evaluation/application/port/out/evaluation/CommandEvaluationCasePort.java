package evaluation.evaluationService.evaluation.application.port.out.evaluation;

import evaluation.evaluationService.evaluation.domain.model.EvaluationCase;

public interface CommandEvaluationCasePort {

    boolean saveIdempotent(EvaluationCase evaluationCase);

    void save(EvaluationCase evaluationCase);

    void update(EvaluationCase evaluationCase);
}
