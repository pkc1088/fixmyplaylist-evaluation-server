package evaluation.evaluationService.evaluation.application.service;

import evaluation.evaluationService.evaluation.application.port.out.dto.RecoveryCompletedEvent;
import evaluation.evaluationService.evaluation.application.port.out.CommandEvaluationCasePort;
import evaluation.evaluationService.evaluation.domain.model.EvaluationCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class EvaluationInboxService {

    private final CommandEvaluationCasePort commandEvaluationCasePort;


    // Don't Start TX Here
    public boolean saveToInboxIdempotent(RecoveryCompletedEvent event) {
        if (event.eventId() == null || event.eventId().isBlank()) {
            return false;
        }

        return commandEvaluationCasePort.saveIdempotent(EvaluationCase.createPending(
                event.eventId(),
                event.targetTitle(),
                event.sourceTitle()
        ));
    }

    @Retryable(
            retryFor = Exception.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 3000)
    )
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateEvaluationResult(EvaluationCase evaluated) {
        commandEvaluationCasePort.update(evaluated);
    }
}
