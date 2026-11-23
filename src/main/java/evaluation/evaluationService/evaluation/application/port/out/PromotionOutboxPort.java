package evaluation.evaluationService.evaluation.application.port.out;

import java.util.List;

public interface PromotionOutboxPort {

    boolean enqueue(String referenceCaseId);

    List<String> loadPendingReferenceCaseIds();

    void markIndexed(String referenceCaseId);

    void markFailed(String referenceCaseId, String failureType);
}
