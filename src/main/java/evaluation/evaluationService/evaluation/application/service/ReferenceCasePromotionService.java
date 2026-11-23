package evaluation.evaluationService.evaluation.application.service;

import evaluation.evaluationService.evaluation.application.port.in.PromotionUseCase;
import evaluation.evaluationService.evaluation.application.port.in.dto.IndexRetryResult;
import evaluation.evaluationService.evaluation.application.port.out.PromotionOutboxPort;
import evaluation.evaluationService.evaluation.application.port.out.QueryReferenceCasePort;
import evaluation.evaluationService.evaluation.application.port.out.RetrieveReferenceCasePort;
import evaluation.evaluationService.evaluation.domain.model.ReferenceCase;
import evaluation.evaluationService.evaluation.domain.model.enums.EvaluationLabel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReferenceCasePromotionService implements PromotionUseCase {

    private final PromotionPersistenceService promotionPersistenceService;
    private final RetrieveReferenceCasePort retrieveReferenceCasePort;
    private final QueryReferenceCasePort queryReferenceCasePort;
    private final PromotionOutboxPort promotionOutboxPort;


    @Override
    public IndexRetryResult promote(String evaluationCaseId, EvaluationLabel humanLabel, String humanReason) {
        return promotionPersistenceService.reviewAndQueue(evaluationCaseId, humanLabel, humanReason)
                .map(promotionCaseId -> indexPending(List.of(promotionCaseId)))
                .orElseGet(() -> new IndexRetryResult(0, 0));
    }

    @Override
    public IndexRetryResult retryPendingIndexing() {
        return indexPending(promotionOutboxPort.loadPendingReferenceCaseIds());
    }

    private IndexRetryResult indexPending(List<String> referenceCaseIds) {
        if (referenceCaseIds.isEmpty()) return new IndexRetryResult(0, 0);

        int indexed = 0;
        int failed = 0;
        for (ReferenceCase referenceCase : queryReferenceCasePort.loadByIds(referenceCaseIds)) {
            String referenceCaseId = referenceCase.getReferenceCaseId();

            try {
                retrieveReferenceCasePort.index(List.of(referenceCase));

            } catch (Exception e) {
                log.error("ReferenceCase Qdrant indexing failed; referenceCaseId={}", referenceCaseId, e);
                markFailedSafely(referenceCaseId, e.getClass().getSimpleName());
                failed++;
                continue;
            }

            try {
                promotionOutboxPort.markIndexed(referenceCaseId);
                indexed++;

            } catch (Exception e) {
                log.error("Qdrant indexing succeeded but failed to mark outbox as INDEXED; referenceCaseId={}", referenceCaseId, e);
                failed++;
            }
        }

        return new IndexRetryResult(indexed, failed);
    }

    private void markFailedSafely(String referenceCaseId, String failureType) {
        try {
            promotionOutboxPort.markFailed(referenceCaseId, failureType);

        } catch (Exception markException) {

            log.error("Failed to update promotion outbox status to FAILED: promotionCaseId={}", referenceCaseId, markException);
        }
    }
}