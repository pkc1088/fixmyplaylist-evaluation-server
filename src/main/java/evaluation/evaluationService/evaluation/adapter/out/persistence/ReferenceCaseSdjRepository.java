package evaluation.evaluationService.evaluation.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReferenceCaseSdjRepository extends JpaRepository<ReferenceCaseJpaEntity, String> {
}