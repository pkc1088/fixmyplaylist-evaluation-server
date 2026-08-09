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
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
            List<RecoveryCase> readCases = loadRecoveryCasePort.read();
            allCases = new ArrayList<>(readCases);

        } catch(IOException | CsvException e) {

            log.error(e.getMessage(), e);
            return;
        }

        Collections.shuffle(allCases, new Random(18));

        List<RecoveryCase> reference = allCases.subList(0, 250);
        List<RecoveryCase> test = allCases.subList(250, 255);// (250, 350);

        retrieveReferenceCasePort.index(reference);

        ExecutorService executor = Executors.newFixedThreadPool(3);//(20);

        try {
            List<CompletableFuture<Optional<EvaluationOutput>>> futures = test.stream()
                    .map(testCase -> CompletableFuture.supplyAsync(() -> processSingleCase(testCase), executor)
                            .orTimeout(30, TimeUnit.SECONDS)
                            .exceptionally(throwable -> {
                                log.error("케이스 평가 타임아웃 또는 에러 - id: {}, 사유: {}", testCase.id(), throwable.getMessage());
                                return Optional.empty();
                            })
                    )
                    .toList();

            List<EvaluationOutput> results = futures.stream()
                    .map(CompletableFuture::join)
                    .flatMap(Optional::stream)
                    .toList();

            int failedCount = test.size() - results.size();
            if (failedCount > 0) {
                log.warn("{}건 중 {}건 평가 실패, {}건만 저장.", test.size(), failedCount, results.size());
            }

            saveRecoveryCasePort.exportResults(results);

        } catch (IOException e) {
            log.error(e.getMessage(), e);

        } finally {
            executor.shutdown();
        }
    }

    private Optional<EvaluationOutput> processSingleCase(RecoveryCase testCase) {
        try {
            log.info("케이스 평가 시작 - id: {}", testCase.id());

            EvaluationResult zeroShot = evaluateRecoveryPort.evaluateZeroShot(testCase);

            List<RetrievedCase> similarCases = retrieveReferenceCasePort.retrieve(testCase, 5);

            EvaluationResult rag = evaluateRecoveryPort.evaluateWithRag(testCase, similarCases);

            return Optional.of(EvaluationOutput.from(
                    testCase,
                    zeroShot,
                    rag,
                    similarCases
            ));

        } catch (Exception e) {
            log.error("케이스 평가 실패 - id: {}, 사유: {}", testCase.id(), e.getMessage(), e);
            return Optional.empty();
        }
    }
}

