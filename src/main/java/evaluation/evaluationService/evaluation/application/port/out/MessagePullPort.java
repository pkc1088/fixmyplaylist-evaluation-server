package evaluation.evaluationService.evaluation.application.port.out;

public interface MessagePullPort {

    int pullAndProcess(EventProcessor processor);
}
