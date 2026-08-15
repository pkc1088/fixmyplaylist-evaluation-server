package evaluation.evaluationService.evaluation.infrastructure.exception;

import evaluation.evaluationService.evaluation.domain.exception.ReferenceCaseSetupException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    @ResponseBody
    @ExceptionHandler(ReferenceCaseSetupException.class)
    public String handleReferenceManualSetupException(ReferenceCaseSetupException e) {
        log.warn("[ReferenceCaseSetupException] {}", e.getMessage(), e);
        return e.getMessage();
    }
}
