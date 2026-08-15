package evaluation.evaluationService.evaluation.application.port.out;

public interface DlqPort {

    void sendToDlq(String rawMessage);
}
