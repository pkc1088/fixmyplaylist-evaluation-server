package evaluation.evaluationService.evaluation.adapter.out.persistence;

import evaluation.evaluationService.evaluation.application.port.out.LoadEvaluationCasePort;
import evaluation.evaluationService.evaluation.application.port.out.SaveEvaluationCasePort;
import evaluation.evaluationService.evaluation.domain.model.EvaluationCase;
import evaluation.evaluationService.evaluation.domain.model.enums.EvaluationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class EvaluationCaseJpaAdapter implements SaveEvaluationCasePort, LoadEvaluationCasePort {

    private final EvaluationCaseSdjRepository repository;


    @Override
    public void save(EvaluationCase evaluationCase) {
        repository.save(EvaluationCaseJpaEntity.from(evaluationCase));
    }

    @Override
    public boolean existsById(String recoveryId) {
        return repository.existsById(recoveryId);
    }

    @Override
    public List<EvaluationCase> loadPendingEvaluation() {
        return repository.findByEvaluationStatus(EvaluationStatus.PENDING).stream()
                .map(EvaluationCaseJpaEntity::toDomain)
                .toList();
    }

    @Override
    public List<EvaluationCase> loadPendingReview() {
        return repository.findByEvaluationStatusOrderByAiConfidenceAsc(EvaluationStatus.AI_EVALUATED).stream()
                .map(EvaluationCaseJpaEntity::toDomain)
                .toList();
    }
}
