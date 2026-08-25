package evaluation.evaluationService.evaluation.adapter.out.persistence;

import evaluation.evaluationService.evaluation.domain.model.enums.EvaluationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvaluationCaseSdjRepository extends JpaRepository<EvaluationCaseJpaEntity, String> {

    List<EvaluationCaseJpaEntity> findByEvaluationStatusIn(List<EvaluationStatus> status);

    List<EvaluationCaseJpaEntity> findByEvaluationStatusOrderByAiConfidenceAsc(EvaluationStatus status);
}
