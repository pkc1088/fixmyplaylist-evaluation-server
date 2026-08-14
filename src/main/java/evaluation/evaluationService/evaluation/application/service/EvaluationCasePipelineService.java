package evaluation.evaluationService.evaluation.application.service;

import evaluation.evaluationService.evaluation.application.port.in.EvaluationCaseUseCase;
import evaluation.evaluationService.evaluation.application.port.out.EvaluateRecoveryPort;
import evaluation.evaluationService.evaluation.application.port.out.dto.VectorSearchResult;
import evaluation.evaluationService.evaluation.application.port.out.CommandEvaluationCasePort;
import evaluation.evaluationService.evaluation.application.port.out.QueryEvaluationCasePort;
import evaluation.evaluationService.evaluation.application.port.out.RetrieveReferenceCasePort;
import evaluation.evaluationService.evaluation.application.port.out.dto.EvaluationResult;
import evaluation.evaluationService.evaluation.application.port.out.QueryReferenceCasePort;
import evaluation.evaluationService.evaluation.domain.model.EvaluationCase;
import evaluation.evaluationService.evaluation.domain.model.ReferenceCase;
import evaluation.evaluationService.evaluation.domain.model.vo.ReferenceTrace;
import evaluation.evaluationService.evaluation.domain.model.vo.RetrievedCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static evaluation.evaluationService.evaluation.domain.model.ReferenceCase.RAG_TOP_K;

@Slf4j
@Service
@RequiredArgsConstructor
public class EvaluationCasePipelineService implements EvaluationCaseUseCase {

    private final CommandEvaluationCasePort commandEvaluationCasePort;
    private final RetrieveReferenceCasePort retrieveReferenceCasePort;
    private final QueryEvaluationCasePort queryEvaluationCasePort;
    private final QueryReferenceCasePort queryReferenceCasePort;
    private final EvaluateRecoveryPort evaluateRecoveryPort;


    public void evaluatePendingCases() {
        // loadPendingEvaluation 가 진입점이 아니라 카프카 수동 풀링이 먼저 나오고
        // '복구 서버'가 발행한 신규 데이터들을 컨슘해서  EvaluationCase 테이블에 저장해서 Inbox 로 중복 수신 체크하고,
        // RAG+LLM 기반 평가 수행한뒤 해당 신규 데이터들의 ai_label, ai_confidence 및 status 업데이트하고,
        // 그 이후 loadPendingEvaluation()로 싹 긁어와서 다시 파이프라인 수행해서 비정상 케이스(PENDING)에 대해 처리(at-least-once)가 맞긴함.
        List<EvaluationCase> pendingCases = queryEvaluationCasePort.loadPendingEvaluation();
        log.info("평가 대상 {}건 로드", pendingCases.size());

        for (EvaluationCase evaluationCase : pendingCases) {
            try {
                evaluateOne(evaluationCase);

            } catch (Exception e) {
                log.error("평가 실패 recoveryId={}", evaluationCase.getEvaluationCaseId(), e);
            }
        }
    }

    private void evaluateOne(EvaluationCase evaluationCase) {

        List<VectorSearchResult> vectorResults = retrieveReferenceCasePort.retrieveIds(evaluationCase, RAG_TOP_K);

        List<String> caseIds = vectorResults.stream()
                .map(VectorSearchResult::referenceCaseId)
                .toList();

        Map<String, ReferenceCase> caseById = queryReferenceCasePort.loadByIds(caseIds).stream()
                .collect(Collectors.toMap(ReferenceCase::getReferenceCaseId, Function.identity()));

        List<RetrievedCase> similarCases = vectorResults.stream()
                .map(vr -> {
                    ReferenceCase rc = caseById.get(vr.referenceCaseId());
                    return (rc != null) ? new RetrievedCase(rc, vr.similarityScore()) : null;
                })
                .filter(Objects::nonNull)
                .toList();

        EvaluationResult result = evaluateRecoveryPort.evaluateWithRag(evaluationCase, similarCases);

        List<ReferenceTrace> refInfo = similarCases.stream()
                .map(rc -> new ReferenceTrace(rc.referenceCase().getReferenceCaseId(), rc.similarityScore()))
                .toList();

        EvaluationCase evaluated = evaluationCase.applyAiEvaluation(
                result.label(),
                result.confidence(),
                refInfo
        );
        commandEvaluationCasePort.update(evaluated);

        log.info("평가 완료 recoveryId={} label={} similarityScore={} 참조건수={}",
                evaluationCase.getEvaluationCaseId(), result.label(), result.confidence(), similarCases.size()
        );
    }
}
