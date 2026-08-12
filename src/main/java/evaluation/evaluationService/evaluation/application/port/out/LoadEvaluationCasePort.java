package evaluation.evaluationService.evaluation.application.port.out;

import evaluation.evaluationService.evaluation.domain.model.EvaluationCase;

import java.util.List;

public interface LoadEvaluationCasePort {

    List<EvaluationCase> loadPendingEvaluation();   // status = PENDING → AI 평가 대상

    List<EvaluationCase> loadPendingReview();       // status = AI_EVALUATED → 휴먼 리뷰 대상
}
