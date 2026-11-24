package evaluation.evaluationService.evaluation.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PromotionOutboxSdjRepository extends JpaRepository<PromotionOutboxJpaEntity, String> {

    List<PromotionOutboxJpaEntity> findByStatusInOrderByCreatedAtAsc(List<PromotionOutboxJpaEntity.Status> statuses);
}
