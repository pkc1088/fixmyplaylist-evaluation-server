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
public class PromotionOutboxService {

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
            // 타임아웃이나 네트워크 오류로 Qdrant 색인 실패 시, 같은 카드로 버튼을 한 번 더 눌렀을 때 오류를 뱉지 않고 이어서 처리.
            return promotionOutboxPort.needsIndexing(evaluationCaseId) ? Optional.of(evaluationCaseId) : Optional.empty();
        }

        // 일반적인 Promotion(AI_EVALUATED -> HUMAN_REVIEWED)
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
        promotionOutboxPort.enqueue(referenceCase.getReferenceCaseId());

        return Optional.of(referenceCase.getReferenceCaseId());
    }
}
