package evaluation.evaluationService.evaluation.adapter.in;

import evaluation.evaluationService.evaluation.application.port.in.RunEvaluationUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/evaluation")
public class EvaluationController {

    private final RunEvaluationUseCase runEvaluationUseCase;


    @PostMapping("/run")
    public ResponseEntity<String> runEvaluation() {
        log.info("[Evaluation Start]");

        runEvaluationUseCase.run();

        log.info("[Evaluation Done]");

        return ResponseEntity.ok("Evaluation Task executed");
    }
}
