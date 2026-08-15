package evaluation.evaluationService.evaluation.application.service;

import evaluation.evaluationService.evaluation.application.port.out.CommandEvaluationCasePort;
import evaluation.evaluationService.evaluation.application.port.out.RetrieveReferenceCasePort;
import evaluation.evaluationService.evaluation.application.port.out.CommandReferenceCasePort;
import evaluation.evaluationService.evaluation.domain.model.EvaluationCase;
import evaluation.evaluationService.evaluation.domain.model.ReferenceCase;
import evaluation.evaluationService.evaluation.domain.model.enums.EvaluationLabel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReferenceCasePromotionService {

    private final RetrieveReferenceCasePort retrieveReferenceCasePort;
    private final CommandEvaluationCasePort commandEvaluationCasePort;
    private final CommandReferenceCasePort commandReferenceCasePort;


    @Transactional(rollbackFor = Exception.class)
    public void promote(
            EvaluationCase evaluationCase,
            EvaluationLabel humanLabel,
            String humanReason
    ) {
        EvaluationCase reviewed = evaluationCase.applyHumanReview(humanLabel, humanReason);

        ReferenceCase referenceCase = ReferenceCase.create(
                reviewed.getEvaluationCaseId(), // evaluationCaseId == referenceCaseId
                reviewed.getTargetTitle(),
                reviewed.getSourceTitle(),
                reviewed.getHumanLabel(),
                reviewed.getHumanReason()
        );

        // outbox 패턴 필요함
        // save 는 isNew = true 강제함. 평가를 재수정하면 문제 생길수도 (근데 그건 Promote 역할이 아니라 다른 클래스기 처리하게 하면 됨)
        commandReferenceCasePort.save(referenceCase);               // 1. CloudSQL: source of truth
        commandEvaluationCasePort.update(reviewed);                 // 2. EvaluationCase → HUMAN_REVIEWED
        retrieveReferenceCasePort.index(List.of(referenceCase));    // 3. Qdrant: 색인
    }
}