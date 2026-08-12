package evaluation.evaluationService.evaluation.application.service;

import evaluation.evaluationService.evaluation.adapter.out.external.GeminiEvaluator;
import evaluation.evaluationService.evaluation.application.port.out.LoadEvaluationCasePort;
import evaluation.evaluationService.evaluation.application.port.out.RetrieveReferenceCasePort;
import evaluation.evaluationService.evaluation.application.port.out.SaveEvaluationCasePort;
import evaluation.evaluationService.evaluation.application.port.out.dto.EvaluationResult;
import evaluation.evaluationService.evaluation.domain.model.EvaluationCase;
import evaluation.evaluationService.evaluation.domain.model.vo.RetrievedCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecoveryEvaluationService {

    private static final int RAG_TOP_K = 5;

    private final LoadEvaluationCasePort loadEvaluationCasePort;
    private final SaveEvaluationCasePort saveEvaluationCasePort;
    private final RetrieveReferenceCasePort retrieveReferenceCasePort;
    private final GeminiEvaluator geminiEvaluator;


    public void evaluatePendingCases() {
        List<EvaluationCase> pendingCases = loadEvaluationCasePort.loadPendingEvaluation();
        log.info("평가 대상 {}건 로드", pendingCases.size());

        for (EvaluationCase evaluationCase : pendingCases) {
            try {
                evaluateOne(evaluationCase);

            } catch (Exception e) {
                log.error("평가 실패 recoveryId={}", evaluationCase.getRecoveryId(), e);
            }
        }
    }

    private void evaluateOne(EvaluationCase evaluationCase) {
        List<RetrievedCase> similarCases = retrieveReferenceCasePort.retrieve(evaluationCase, RAG_TOP_K);

        EvaluationResult result = geminiEvaluator.evaluateWithRag(evaluationCase, similarCases);

        EvaluationCase evaluated = evaluationCase.applyAiEvaluation(result.label(), result.confidence());
        saveEvaluationCasePort.save(evaluated);

        log.info("평가 완료 recoveryId={} label={} confidence={} 참조건수={}",
                evaluationCase.getRecoveryId(), result.label(), result.confidence(), similarCases.size()
        );
    }

}
