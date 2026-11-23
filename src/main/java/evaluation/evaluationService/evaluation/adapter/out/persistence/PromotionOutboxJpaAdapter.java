package evaluation.evaluationService.evaluation.adapter.out.persistence;

import evaluation.evaluationService.evaluation.application.port.out.PromotionOutboxPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PromotionOutboxJpaAdapter implements PromotionOutboxPort {

    private final PromotionOutboxSdjRepository repository;


    @Override
    public void enqueue(String referenceCaseId) {
        repository.save(PromotionOutboxJpaEntity.createPending(referenceCaseId));
    }

    @Override
    public boolean needsIndexing(String referenceCaseId) {
        return repository.findById(referenceCaseId)
                .map(outbox -> outbox.getStatus() != PromotionOutboxJpaEntity.Status.INDEXED)
                .orElseThrow(() -> new IllegalStateException("Promotion outbox missing; referenceCaseId=" + referenceCaseId));
    }

    @Override
    public List<String> loadPendingReferenceCaseIds() {
        return repository.findByStatusInOrderByCreatedAtAsc(
                List.of(
                        PromotionOutboxJpaEntity.Status.PENDING,
                        PromotionOutboxJpaEntity.Status.FAILED
                )).stream()
                .map(PromotionOutboxJpaEntity::getReferenceCaseId)
                .toList();
    }

    @Override
    @Transactional
    public void markIndexed(String referenceCaseId) {
        repository.findById(referenceCaseId).ifPresent(PromotionOutboxJpaEntity::markIndexed);
    }

    @Override
    @Transactional
    public void markFailed(String referenceCaseId, String failureType) {
        repository.findById(referenceCaseId).ifPresent(outbox -> outbox.markFailed(failureType));
    }
}
