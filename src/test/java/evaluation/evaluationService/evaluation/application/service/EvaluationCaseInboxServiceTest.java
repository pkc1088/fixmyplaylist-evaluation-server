package evaluation.evaluationService.evaluation.application.service;

import evaluation.evaluationService.evaluation.application.port.out.CommandEvaluationCasePort;
import evaluation.evaluationService.evaluation.application.port.out.dto.RecoveryCompletedEvent;
import evaluation.evaluationService.evaluation.domain.model.EvaluationCase;
import evaluation.evaluationService.evaluation.domain.model.enums.EvaluationStatus;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class EvaluationCaseInboxServiceTest {

    @Mock private CommandEvaluationCasePort commandEvaluationCasePort;
    @InjectMocks private EvaluationCaseInboxService service;

    @Test
    void savesValidEventAsPendingAndReturnsPortResult() {
        when(commandEvaluationCasePort.saveIdempotent(any(EvaluationCase.class)))
                .thenReturn(true, false);

        assertTrue(service.saveToInboxIdempotent(new RecoveryCompletedEvent("event-1", "Target", "Source")));
        assertFalse(service.saveToInboxIdempotent(new RecoveryCompletedEvent("event-1", "Target", "Source")));

        ArgumentCaptor<EvaluationCase> captor = ArgumentCaptor.forClass(EvaluationCase.class);
        verify(commandEvaluationCasePort, times(2)).saveIdempotent(captor.capture());
        EvaluationCase saved = captor.getAllValues().get(0);
        assertEquals("event-1", saved.getEvaluationCaseId());
        assertEquals("Target", saved.getTargetTitle());
        assertEquals("Source", saved.getSourceTitle());
        assertEquals(EvaluationStatus.PENDING, saved.getEvaluationStatus());
    }

    @Test
    void returnsFalseForNullOrBlankEventIdWithoutCallingPort() {
        assertFalse(service.saveToInboxIdempotent(new RecoveryCompletedEvent(null, "Target", "Source")));
        assertFalse(service.saveToInboxIdempotent(new RecoveryCompletedEvent("  ", "Target", "Source")));

        verifyNoInteractions(commandEvaluationCasePort);
    }

    @Test
    void throwsOnOverlongEventIdOrBlankTitlesSoCallerRoutesToDlq() {
        assertThrows(IllegalArgumentException.class,
                () -> service.saveToInboxIdempotent(new RecoveryCompletedEvent("a".repeat(101), "Target", "Source")));
        assertThrows(IllegalArgumentException.class,
                () -> service.saveToInboxIdempotent(new RecoveryCompletedEvent("event-1", "  ", "Source")));
        assertThrows(IllegalArgumentException.class,
                () -> service.saveToInboxIdempotent(new RecoveryCompletedEvent("event-1", "Target", "  ")));

        verifyNoInteractions(commandEvaluationCasePort);
    }

    @Test
    void updateEvaluationResultDelegatesToCommandPort() {
        EvaluationCase evaluated = EvaluationCase.createPending("event-2", "Target", "Source");

        service.updateEvaluationResult(evaluated);

        verify(commandEvaluationCasePort).update(evaluated);
    }
}
