package evaluation.evaluationService.evaluation.infrastructure.exception;

import evaluation.evaluationService.evaluation.domain.exception.ReferenceCaseSetupException;
import evaluation.evaluationService.evaluation.domain.exception.EvaluationCaseNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    @ExceptionHandler(EvaluationCaseNotFoundException.class)
    public ResponseEntity<String> handleEvaluationCaseNotFound(EvaluationCaseNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }

    @ResponseBody
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleInvalidState(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }

    @ResponseBody
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleInvalidRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    @ResponseBody
    @ExceptionHandler(ReferenceCaseSetupException.class)
    public String handleReferenceManualSetupException(ReferenceCaseSetupException e) {
        log.warn("[ReferenceCaseSetupException] {}", e.getMessage(), e);
        return e.getMessage();
    }
}
