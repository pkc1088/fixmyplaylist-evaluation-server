package evaluation.evaluationService.evaluation.application.service;

import evaluation.evaluationService.evaluation.adapter.out.external.GeminiEvaluator;
import evaluation.evaluationService.evaluation.application.port.out.dto.VectorSearchResult;
import evaluation.evaluationService.evaluation.application.port.out.evaluation.CommandEvaluationCasePort;
import evaluation.evaluationService.evaluation.application.port.out.evaluation.QueryEvaluationCasePort;
import evaluation.evaluationService.evaluation.application.port.out.RetrieveReferenceCasePort;
import evaluation.evaluationService.evaluation.application.port.out.dto.EvaluationResult;
import evaluation.evaluationService.evaluation.application.port.out.reference.QueryReferenceCasePort;
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
public class RecoveryEvaluationService {

    private final CommandEvaluationCasePort commandEvaluationCasePort;
    private final RetrieveReferenceCasePort retrieveReferenceCasePort;
    private final QueryEvaluationCasePort queryEvaluationCasePort;
    private final QueryReferenceCasePort queryReferenceCasePort;
    private final GeminiEvaluator geminiEvaluator;


    public void evaluatePendingCases() {
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
                    return (rc != null) ? new RetrievedCase(rc, vr.confidence()) : null;
                })
                .filter(Objects::nonNull)
                .toList();

        EvaluationResult result = geminiEvaluator.evaluateWithRag(evaluationCase, similarCases);

        List<ReferenceTrace> refInfo = similarCases.stream()
                .map(rc -> new ReferenceTrace(rc.referenceCase().getReferenceCaseId(), rc.similarityScore()))
                .toList();

        EvaluationCase evaluated = evaluationCase.applyAiEvaluation(
                result.label(),
                result.confidence(),
                refInfo
        );
        commandEvaluationCasePort.update(evaluated);

        log.info("평가 완료 recoveryId={} label={} confidence={} 참조건수={}",
                evaluationCase.getEvaluationCaseId(), result.label(), result.confidence(), similarCases.size()
        );
    }

    private void setUp() {
        // 0. 휴먼 라벨링 완료된 CSV 읽어서 DB 에 저장

        // 1. 휴먼 라벨링 완료된 CSV 읽어서 임베딩(index) 후 Qdrant 에 저장 (초기 셋업)

        // 2. 로컬 DB 에서 EvaluationCase 중 Pending 된 애들 읽기

        // 3. 벡터화 시켜서 retrieve 하기

        // 4. RAG 으로 확보한 Top K 를 추가 정보한 뒤 LLM 호출해서 응답받기

        // 5. 응답 파싱 후 EvaluationCase 상태(AiLabel, Confidence) 업데이트

        // 6. Evaluation 을 ReferenceCase 로 승격 시키는 로직은 차후 구현
    }
}
