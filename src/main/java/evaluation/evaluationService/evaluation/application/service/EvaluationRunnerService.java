package evaluation.evaluationService.evaluation.application.service;

import com.opencsv.exceptions.CsvException;
import evaluation.evaluationService.evaluation.application.port.in.RunEvaluationUseCase;
import evaluation.evaluationService.evaluation.application.port.out.EvaluateRecoveryPort;
import evaluation.evaluationService.evaluation.application.port.out.LoadRecoveryCasePort;
import evaluation.evaluationService.evaluation.application.port.out.RetrieveReferenceCasePort;
import evaluation.evaluationService.evaluation.application.port.out.SaveRecoveryCasePort;
import evaluation.evaluationService.evaluation.domain.model.vo.EvaluationOutput;
import evaluation.evaluationService.evaluation.domain.model.vo.EvaluationResult;
import evaluation.evaluationService.evaluation.domain.model.vo.RecoveryCase;
import evaluation.evaluationService.evaluation.domain.model.vo.RetrievedCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
@RequiredArgsConstructor
public class EvaluationRunnerService implements RunEvaluationUseCase {

    private final RetrieveReferenceCasePort retrieveReferenceCasePort;
    private final LoadRecoveryCasePort loadRecoveryCasePort;
    private final SaveRecoveryCasePort saveRecoveryCasePort;
    private final EvaluateRecoveryPort evaluateRecoveryPort;


    public void run() {

        List<RecoveryCase> allCases;
        try {
            List<RecoveryCase> readCases = loadRecoveryCasePort.read(Path.of("data/recovery_cases.csv"));
            allCases = new ArrayList<>(readCases);

        } catch(IOException | CsvException e) {

            log.error(e.getMessage(), e);
            return;
        }

        Collections.shuffle(allCases, new Random(44));

        List<RecoveryCase> reference = allCases.subList(0, 250);
        List<RecoveryCase> test = allCases.subList(250, 255);// (250, 350);

        retrieveReferenceCasePort.index(reference);

        ExecutorService executor = Executors.newFixedThreadPool(3);//(20);

        try {
            List<CompletableFuture<EvaluationOutput>> futures = test.stream()
                    .map(testCase ->
                            CompletableFuture.supplyAsync(
                                    () -> processSingleCase(testCase),
                                    executor
                            )
                    )
                    .toList();

            List<EvaluationOutput> results = futures.stream()
                    .map(CompletableFuture::join)
                    .toList();

            saveRecoveryCasePort.exportResults(results);

        } catch (IOException e) {
            log.error(e.getMessage(), e);

        } finally {
            executor.shutdown();
        }
    }

    private EvaluationOutput processSingleCase(RecoveryCase testCase) {

        EvaluationResult zeroShot = evaluateRecoveryPort.evaluateZeroShot(testCase);

        List<RetrievedCase> similarCases = retrieveReferenceCasePort.retrieve(testCase, 5);

        EvaluationResult rag = evaluateRecoveryPort.evaluateWithRag(testCase, similarCases);

        return EvaluationOutput.from(
                testCase,
                zeroShot,
                rag,
                similarCases
        );
    }
}

