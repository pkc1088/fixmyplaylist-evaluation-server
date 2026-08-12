package evaluation.evaluationService.evaluation.adapter.out.persistence;

import evaluation.evaluationService.evaluation.application.port.out.evaluation.CommandEvaluationCasePort;
import evaluation.evaluationService.evaluation.application.port.out.evaluation.QueryEvaluationCasePort;
import evaluation.evaluationService.evaluation.domain.model.EvaluationCase;
import evaluation.evaluationService.evaluation.domain.model.enums.EvaluationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class EvaluationCaseJpaAdapter implements CommandEvaluationCasePort, QueryEvaluationCasePort {

    private final EvaluationCaseSdjRepository repository;
    private final EvaluationCaseMapper mapper;


    @Override
    public void save(EvaluationCase evaluationCase) {
        repository.save(mapper.toEntity(evaluationCase, true));
    }

    @Override
    public void update(EvaluationCase evaluationCase) {
        repository.save(mapper.toEntity(evaluationCase, false));
    }

    @Override
    public boolean existsById(String recoveryId) {
        return repository.existsById(recoveryId);
    }

    @Override
    public List<EvaluationCase> loadPendingEvaluation() {
        return repository.findByEvaluationStatus(EvaluationStatus.PENDING).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<EvaluationCase> loadPendingReview() {
        return repository.findByEvaluationStatusOrderByAiConfidenceAsc(EvaluationStatus.AI_EVALUATED).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
