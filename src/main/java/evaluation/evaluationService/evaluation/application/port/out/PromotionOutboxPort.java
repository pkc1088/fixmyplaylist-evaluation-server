package evaluation.evaluationService.evaluation.application.port.out;

import java.util.List;

public interface PromotionOutboxPort {

    void enqueue(String referenceCaseId);

    boolean needsIndexing(String referenceCaseId);

    List<String> loadPendingReferenceCaseIds();

    void markIndexed(String referenceCaseId);

    void markFailed(String referenceCaseId, String failureType);
}
