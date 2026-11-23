package evaluation.evaluationService.evaluation.application.service;

import evaluation.evaluationService.evaluation.application.port.out.CommandEvaluationCasePort;
import evaluation.evaluationService.evaluation.application.port.out.CommandReferenceCasePort;
import evaluation.evaluationService.evaluation.application.port.out.PromotionOutboxPort;
import evaluation.evaluationService.evaluation.application.port.out.QueryEvaluationCasePort;
import evaluation.evaluationService.evaluation.domain.model.EvaluationCase;
import evaluation.evaluationService.evaluation.domain.model.ReferenceCase;
import evaluation.evaluationService.evaluation.domain.model.enums.EvaluationLabel;
import evaluation.evaluationService.evaluation.domain.model.enums.EvaluationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PromotionPersistenceService {

    private final CommandEvaluationCasePort commandEvaluationCasePort;
    private final CommandReferenceCasePort commandReferenceCasePort;
    private final QueryEvaluationCasePort queryEvaluationCasePort;
    private final PromotionOutboxPort promotionOutboxPort;


    @Transactional
    public Optional<String> reviewAndQueue(String evaluationCaseId, EvaluationLabel humanLabel, String humanReason) {

        EvaluationCase evaluationCase = queryEvaluationCasePort.loadById(evaluationCaseId);

        if (evaluationCase.getEvaluationStatus() == EvaluationStatus.HUMAN_REVIEWED) {
            // 이미 검수됐지만 라벨이나 사유가 다르면 예외 던져서, 기존 검수 결과를 조용히 덮어쓰지 않도록 함.
            if (evaluationCase.getHumanLabel() != humanLabel || !humanReason.equals(evaluationCase.getHumanReason())) {
                throw new IllegalStateException("EvaluationCase was already reviewed with different values");
            }
            // 이미 검수됐고 요청의 라벨·사유가 저장된 값과 같으면, 네트워크 재요청으로 보고 Outbox 를 확인한 뒤 같은 승격 색인을 다시 시도
            boolean needsIndexing = promotionOutboxPort.enqueue(evaluationCaseId);

            return needsIndexing ? Optional.of(evaluationCaseId) : Optional.empty();
        }

        // 아직 검수되지 않은 경우에는, 이 메서드는 AI_EVALUATED 상태만 허용하므로 다른 상태의 사례는 거부.
        evaluationCase.applyHumanReview(humanLabel, humanReason);

        ReferenceCase referenceCase = ReferenceCase.create(
                evaluationCaseId,
                evaluationCase.getTargetTitle(),
                evaluationCase.getSourceTitle(),
                humanLabel,
                humanReason
        );

        commandReferenceCasePort.save(referenceCase);
        commandEvaluationCasePort.update(evaluationCase);
        boolean needsIndexing = promotionOutboxPort.enqueue(referenceCase.getReferenceCaseId());

        return needsIndexing ? Optional.of(referenceCase.getReferenceCaseId()) : Optional.empty();
    }
}
