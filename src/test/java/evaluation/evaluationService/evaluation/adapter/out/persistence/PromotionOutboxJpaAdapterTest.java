package evaluation.evaluationService.evaluation.adapter.out.persistence;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class PromotionOutboxJpaAdapterTest {

    @Mock private PromotionOutboxSdjRepository repository;
    @InjectMocks private PromotionOutboxJpaAdapter adapter;

    @Test
    void enqueuePersistsNewPendingEntity() {
        adapter.enqueue("reference-1");

        ArgumentCaptor<PromotionOutboxJpaEntity> captor = ArgumentCaptor.forClass(PromotionOutboxJpaEntity.class);
        verify(repository).save(captor.capture());
        PromotionOutboxJpaEntity saved = captor.getValue();
        assertEquals("reference-1", saved.getReferenceCaseId());
        assertEquals("reference-1", saved.getId());
        assertEquals(PromotionOutboxJpaEntity.Status.PENDING, saved.getStatus());
        assertEquals(0, saved.getAttempts());
        assertTrue(saved.isNew());

        saved.prePersist();
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
        saved.markNotNew();
        assertFalse(saved.isNew());
    }

    @Test
    void needsIndexingIsTrueUnlessOutboxWasIndexedAndThrowsWhenMissing() {
        when(repository.findById("pending"))
                .thenReturn(Optional.of(outbox("pending", PromotionOutboxJpaEntity.Status.PENDING)));
        when(repository.findById("failed"))
                .thenReturn(Optional.of(outbox("failed", PromotionOutboxJpaEntity.Status.FAILED)));
        when(repository.findById("indexed"))
                .thenReturn(Optional.of(outbox("indexed", PromotionOutboxJpaEntity.Status.INDEXED)));
        when(repository.findById("missing"))
                .thenReturn(Optional.empty());

        assertTrue(adapter.needsIndexing("pending"));
        assertTrue(adapter.needsIndexing("failed"));
        assertFalse(adapter.needsIndexing("indexed"));
        assertThrows(IllegalStateException.class, () -> adapter.needsIndexing("missing"));
    }

    @Test
    void returnsIdsForPendingAndFailedStatuses() {
        when(repository.findByStatusInOrderByCreatedAtAsc(List.of(
                PromotionOutboxJpaEntity.Status.PENDING, PromotionOutboxJpaEntity.Status.FAILED)))
                .thenReturn(List.of(outbox("a", PromotionOutboxJpaEntity.Status.PENDING),
                        outbox("b", PromotionOutboxJpaEntity.Status.FAILED)));

        assertEquals(List.of("a", "b"), adapter.loadPendingReferenceCaseIds());
    }

    @Test
    void marksExistingEntriesIndexedOrFailedAndIgnoresMissingEntries() {
        PromotionOutboxJpaEntity indexed = outbox("indexed", PromotionOutboxJpaEntity.Status.PENDING);
        PromotionOutboxJpaEntity failed = outbox("failed", PromotionOutboxJpaEntity.Status.PENDING);
        when(repository.findById("indexed")).thenReturn(Optional.of(indexed));
        when(repository.findById("failed")).thenReturn(Optional.of(failed));
        when(repository.findById("missing")).thenReturn(Optional.empty());

        adapter.markIndexed("indexed");
        adapter.markFailed("failed", "VectorException");
        adapter.markIndexed("missing");
        adapter.markFailed("missing", "VectorException");

        assertEquals(PromotionOutboxJpaEntity.Status.INDEXED, indexed.getStatus());
        assertNotNull(indexed.getUpdatedAt());
        assertEquals(PromotionOutboxJpaEntity.Status.FAILED, failed.getStatus());
        assertEquals(1, failed.getAttempts());
        assertEquals("VectorException", failed.getLastFailureType());
        assertNotNull(failed.getUpdatedAt());
    }

    @Test
    void markFailedAccumulatesAttemptsAndDoesNotOverwriteIndexedStatus() {
        PromotionOutboxJpaEntity retrying = outbox("retrying", PromotionOutboxJpaEntity.Status.PENDING);
        PromotionOutboxJpaEntity alreadyIndexed = outbox("done", PromotionOutboxJpaEntity.Status.INDEXED);
        when(repository.findById("retrying")).thenReturn(Optional.of(retrying));
        when(repository.findById("done")).thenReturn(Optional.of(alreadyIndexed));

        adapter.markFailed("retrying", "TimeoutException");
        adapter.markFailed("retrying", "StatusRuntimeException");
        adapter.markFailed("done", "LateFailure");

        assertEquals(PromotionOutboxJpaEntity.Status.FAILED, retrying.getStatus());
        assertEquals(2, retrying.getAttempts());
        assertEquals("StatusRuntimeException", retrying.getLastFailureType());

        assertEquals(PromotionOutboxJpaEntity.Status.INDEXED, alreadyIndexed.getStatus());
        assertEquals(0, alreadyIndexed.getAttempts());
        assertNull(alreadyIndexed.getLastFailureType());
    }

    @Test
    void markIndexedClearsPreviousFailureType() {
        PromotionOutboxJpaEntity failed = outbox("recovered", PromotionOutboxJpaEntity.Status.PENDING);
        failed.markFailed("VectorException");
        when(repository.findById("recovered")).thenReturn(Optional.of(failed));

        adapter.markIndexed("recovered");

        assertEquals(PromotionOutboxJpaEntity.Status.INDEXED, failed.getStatus());
        assertNull(failed.getLastFailureType());
        assertNotNull(failed.getUpdatedAt());
    }

    private PromotionOutboxJpaEntity outbox(String id, PromotionOutboxJpaEntity.Status status) {
        return PromotionOutboxJpaEntity.builder().referenceCaseId(id).status(status).build();
    }
}
