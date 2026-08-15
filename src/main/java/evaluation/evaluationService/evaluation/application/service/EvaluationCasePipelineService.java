package evaluation.evaluationService.evaluation.application.service;

import evaluation.evaluationService.evaluation.application.port.in.EvaluationCaseUseCase;
import evaluation.evaluationService.evaluation.application.port.out.*;
import evaluation.evaluationService.evaluation.application.port.out.dto.RecoveryCompletedEvent;
import evaluation.evaluationService.evaluation.application.port.out.dto.VectorSearchResult;
import evaluation.evaluationService.evaluation.application.port.out.dto.EvaluationResult;
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

    private final EvaluationCaseInboxService evaluationCaseInboxService;
    private final RetrieveReferenceCasePort retrieveReferenceCasePort;
    private final QueryEvaluationCasePort queryEvaluationCasePort;
    private final QueryReferenceCasePort queryReferenceCasePort;
    private final EvaluateRecoveryPort evaluateRecoveryPort;
    private final MessagePullPort messagePullPort;
    private final DlqPort dlqPort;


    public void evaluatePendingCases() {

        // Kafka 수동 폴링 — Inbox 적재까지만
        int consumed = messagePullPort.pullAndProcess(new EventProcessor() {

            @Override
            public void process(RecoveryCompletedEvent event) {
                boolean isNew = evaluationCaseInboxService.saveToInboxIdempotent(event);
                if (!isNew) {
                    log.info("이미 수신된 이벤트, 스킵 eventId={}", event.eventId());
                }
            }

            @Override
            public void onFail(String rawMessage) {
                dlqPort.sendToDlq(rawMessage);
            }
        });

        log.info("Kafka 신규 수신 {}건", consumed);

        // PENDING 전체 평가 — 방금 들어온 신규 건 + 과거 장애로 남은 잔여 건
        List<EvaluationCase> pendingCases = queryEvaluationCasePort.loadPendingEvaluation();
        log.info("평가 대상 {}건", pendingCases.size());

        for (EvaluationCase evaluationCase : pendingCases) {
            try {
                evaluateOne(evaluationCase);

            } catch (Exception e) {
                log.error("평가 실패, PENDING 유지(다음 실행에서 재시도) recoveryId={}", evaluationCase.getEvaluationCaseId(), e);
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

        evaluationCaseInboxService.updateEvaluationResult(evaluated);

        log.info("평가 완료: evaluationCaseId={}, label={}, similarityScore={}, 참조건수={}",
                evaluationCase.getEvaluationCaseId(), result.label(), result.confidence(), similarCases.size()
        );
    }
}
