package evaluation.evaluationService.evaluation.application.port.out;

import evaluation.evaluationService.evaluation.application.port.out.dto.RecoveryCompletedEvent;

// 콜백 정의
public interface EventProcessor {
    // 성공 시 할 일
    void process(RecoveryCompletedEvent event) throws Exception;
    // 실패 시 할 일
    void onFail(String rawMessage);
}