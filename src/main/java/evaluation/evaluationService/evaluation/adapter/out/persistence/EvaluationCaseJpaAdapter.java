package evaluation.evaluationService.evaluation.adapter.out.persistence;

import evaluation.evaluationService.evaluation.application.port.out.CommandEvaluationCasePort;
import evaluation.evaluationService.evaluation.application.port.out.QueryEvaluationCasePort;
import evaluation.evaluationService.evaluation.domain.model.EvaluationCase;
import evaluation.evaluationService.evaluation.domain.model.enums.EvaluationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class EvaluationCaseJpaAdapter implements CommandEvaluationCasePort, QueryEvaluationCasePort {

    private final EvaluationCaseSdjRepository repository;
    private final EvaluationCaseMapper mapper;


    // [자동화 단계] EvaluationCase: Inbox
    @Override
    public boolean saveIdempotent(EvaluationCase evaluationCase) {
        try {
            EvaluationCaseJpaEntity entity = mapper.toEntity(evaluationCase, true);
            repository.saveAndFlush(entity);
            return true;

        } catch (DataIntegrityViolationException e) {
            log.warn("[EvaluationCase 중복 수신 무시] id={}", evaluationCase.getEvaluationCaseId());
            return false;
        }
    }

    @Override
    public void save(EvaluationCase evaluationCase) {
        repository.save(mapper.toEntity(evaluationCase, true));
    }

    @Override
    public void update(EvaluationCase evaluationCase) {
        repository.save(mapper.toEntity(evaluationCase, false));
    }

    @Override
    public boolean existsById(String evaluationCaseId) {
        return repository.existsById(evaluationCaseId);
    }

    @Override
    public List<EvaluationCase> loadPendingEvaluation() {
        return repository.findByEvaluationStatusIn(List.of(EvaluationStatus.PENDING, EvaluationStatus.FAILED)).stream()
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
