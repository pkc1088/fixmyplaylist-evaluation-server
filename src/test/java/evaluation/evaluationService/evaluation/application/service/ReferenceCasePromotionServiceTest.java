package evaluation.evaluationService.evaluation.application.service;

import evaluation.evaluationService.evaluation.application.port.in.dto.IndexRetryResult;
import evaluation.evaluationService.evaluation.application.port.out.PromotionOutboxPort;
import evaluation.evaluationService.evaluation.application.port.out.QueryReferenceCasePort;
import evaluation.evaluationService.evaluation.application.port.out.RetrieveReferenceCasePort;
import evaluation.evaluationService.evaluation.domain.model.ReferenceCase;
import evaluation.evaluationService.evaluation.domain.model.enums.EvaluationLabel;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class ReferenceCasePromotionServiceTest {

    @Mock private RetrieveReferenceCasePort retrieveReferenceCasePort;
    @Mock private PromotionOutboxService promotionOutboxService;
    @Mock private QueryReferenceCasePort queryReferenceCasePort;
    @Mock private PromotionOutboxPort promotionOutboxPort;

    @InjectMocks private ReferenceCasePromotionService service;


    @Test
    void promoteQueuesAndIndexesWhenReviewIsNew() {
        ReferenceCase referenceCase = referenceCase("case-1");

        when(promotionOutboxService.reviewAndQueue("case-1", EvaluationLabel.SUCCESS, "검수 근거"))
                .thenReturn(Optional.of("case-1"));
        when(queryReferenceCasePort.loadByIds(List.of("case-1")))
                .thenReturn(List.of(referenceCase));

        IndexRetryResult result = service.promote("case-1", EvaluationLabel.SUCCESS, "검수 근거");

        assertEquals(new IndexRetryResult(1, 0), result);
        verify(retrieveReferenceCasePort).index(List.of(referenceCase));
        verify(promotionOutboxPort).markIndexed("case-1");
    }

    @Test
    void promoteReturnsZeroWhenMatchingReviewWasAlreadyIndexed() {
        when(promotionOutboxService.reviewAndQueue("case-1", EvaluationLabel.SUCCESS, "검수 근거"))
                .thenReturn(Optional.empty());

        assertEquals(new IndexRetryResult(0, 0), service.promote("case-1", EvaluationLabel.SUCCESS, "검수 근거"));

        verifyNoInteractions(retrieveReferenceCasePort, queryReferenceCasePort, promotionOutboxPort);
    }

    @Test
    void indexingFailureMarksOutboxFailedAndCountsFailure() {
        ReferenceCase referenceCase = referenceCase("case-2");

        when(promotionOutboxPort.loadPendingReferenceCaseIds())
                .thenReturn(List.of("case-2"));
        when(queryReferenceCasePort.loadByIds(List.of("case-2")))
                .thenReturn(List.of(referenceCase));
        doThrow(new IllegalArgumentException("index failed"))
                .when(retrieveReferenceCasePort).index(List.of(referenceCase));

        assertEquals(new IndexRetryResult(0, 1), service.retryPendingIndexing());

        verify(promotionOutboxPort).markFailed("case-2", "IllegalArgumentException");
        verify(promotionOutboxPort, never()).markIndexed(anyString());
    }

    @Test
    void outboxFailureAfterSuccessfulIndexCountsFailure() {
        ReferenceCase referenceCase = referenceCase("case-3");

        when(promotionOutboxPort.loadPendingReferenceCaseIds())
                .thenReturn(List.of("case-3"));
        when(queryReferenceCasePort.loadByIds(List.of("case-3")))
                .thenReturn(List.of(referenceCase));
        doThrow(new IllegalStateException("db unavailable"))
                .when(promotionOutboxPort).markIndexed("case-3");

        assertEquals(new IndexRetryResult(0, 1), service.retryPendingIndexing());

        verify(retrieveReferenceCasePort).index(List.of(referenceCase));
        verify(promotionOutboxPort).markIndexed("case-3");
        verify(promotionOutboxPort, never()).markFailed(anyString(), anyString());
    }

    @Test
    void retryUsesPendingIdsAndReturnsZeroWithoutLoadingWhenThereAreNone() {
        when(promotionOutboxPort.loadPendingReferenceCaseIds()).thenReturn(List.of());

        assertEquals(new IndexRetryResult(0, 0), service.retryPendingIndexing());

        verifyNoInteractions(queryReferenceCasePort, retrieveReferenceCasePort);
    }

    @Test
    void retryContinuesAfterOneCaseFails() {
        ReferenceCase failed = referenceCase("case-4");
        ReferenceCase successful = referenceCase("case-5");

        when(promotionOutboxPort.loadPendingReferenceCaseIds())
                .thenReturn(List.of("case-4", "case-5"));
        when(queryReferenceCasePort.loadByIds(List.of("case-4", "case-5")))
                .thenReturn(List.of(failed, successful));
        doThrow(new RuntimeException("qdrant unavailable"))
                .when(retrieveReferenceCasePort).index(List.of(failed));

        assertEquals(new IndexRetryResult(1, 1), service.retryPendingIndexing());

        verify(promotionOutboxPort).markFailed("case-4", "RuntimeException");
        verify(retrieveReferenceCasePort).index(List.of(successful));
        verify(promotionOutboxPort).markIndexed("case-5");
    }

    private ReferenceCase referenceCase(String id) {
        return ReferenceCase.create(id, "Target title", "Source title", EvaluationLabel.SUCCESS, "검수 근거");
    }
}
