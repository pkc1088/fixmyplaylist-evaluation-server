//package evaluation.evaluationService.evaluation.application.service;
//
//import evaluation.evaluationService.evaluation.application.port.in.RunEvaluationUseCase;
//import evaluation.evaluationService.evaluation.application.port.out.EvaluateRecoveryPort;
//import evaluation.evaluationService.evaluation.application.port.out.evaluation.QueryEvaluationCasePort;
//import evaluation.evaluationService.evaluation.application.port.out.RetrieveReferenceCasePort;
//import evaluation.evaluationService.evaluation.application.port.out.evaluation.CommandEvaluationCasePort;
//import evaluation.evaluationService.evaluation.domain.model.EvaluationCase;
//import evaluation.evaluationService.evaluation.domain.model.ReferenceCase;
//import evaluation.evaluationService.evaluation.domain.model.vo.EvaluationOutput;
//import evaluation.evaluationService.evaluation.application.port.out.dto.EvaluationResult;
//import evaluation.evaluationService.evaluation.domain.model.vo.RetrievedCase;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Component;
//
//import java.io.IOException;
//import java.util.*;
//import java.util.concurrent.CompletableFuture;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.TimeUnit;
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class EvaluationRunnerService implements RunEvaluationUseCase {
//
//    private final RetrieveReferenceCasePort retrieveReferenceCasePort;
//    private final QueryEvaluationCasePort loadRecoveryCasePort;
//    private final CommandEvaluationCasePort saveRecoveryCasePort;
//    private final EvaluateRecoveryPort evaluateRecoveryPort;
//
//
//    public void run() {
//
///*        List<ReferenceCase> allCases;
//        try {
//            List<ReferenceCase> readCases = loadRecoveryCasePort.read();
//            allCases = new ArrayList<>(readCases);
//
//        } catch(IOException | CsvException e) {
//
//            log.error(e.getMessage(), e);
//            return;
//        }
//
//        Collections.shuffle(allCases, new Random(18));
//
//        List<ReferenceCase> reference = allCases.subList(0, 245);
//        List<ReferenceCase> test = allCases.subList(245, 345);
//
//        retrieveReferenceCasePort.index(reference);
//*/
//        List<ReferenceCase> referenceCases;
//        List<ReferenceCase> testCases;
//
//        try {
//            // 1. 초기 데이터 로드 (CSVReader Adapter 등에서 ReferenceCase 형태로 변환하여 읽음)
//            List<ReferenceCase> allReferenceCases = loadRecoveryCasePort.read();
//            List<ReferenceCase> mutableCases = new ArrayList<>(allReferenceCases);
//
//            Collections.shuffle(mutableCases, new Random(18));
//
//            // 245건은 RAG용 Reference 데이터로, 100건은 평가 대상(Evaluation) 데이터로 분리
//            referenceCases = mutableCases.subList(0, 245);
//            testCases = mutableCases.subList(245, 345);
//            // 테스트용 데이터는 ReferenceCase 목록 기반으로 EvaluationCase 객체 생성
//
//        } catch (Exception e) {
//            log.error("데이터 로딩 실패: {}", e.getMessage(), e);
//            return;
//        }
//
//        // 2. Qdrant 에 ReferenceCase 245건 임베딩 및 인덱싱
//        retrieveReferenceCasePort.index(referenceCases);
//
//        ExecutorService executor = Executors.newFixedThreadPool(10);
//
//        try {
//            List<CompletableFuture<Optional<EvaluationOutput>>> futures = testCases.stream()
//                    .map(testCase -> CompletableFuture.supplyAsync(() -> processSingleCase(testCase), executor)
//                            .orTimeout(30, TimeUnit.SECONDS)
//                            .exceptionally(throwable -> {
//                                log.error("케이스 평가 타임아웃 또는 에러 - id: {}, 사유: {}", testCase.getReferenceCaseId(), throwable.getMessage());
//                                return Optional.empty();
//                            })
//                    )
//                    .toList();
//
//            List<EvaluationOutput> results = futures.stream()
//                    .map(CompletableFuture::join)
//                    .flatMap(Optional::stream)
//                    .toList();
//
//            int failedCount = testCases.size() - results.size();
//            if (failedCount > 0) {
//                log.warn("{}건 중 {}건 평가 실패, {}건만 저장.", testCases.size(), failedCount, results.size());
//            }
//
//            saveRecoveryCasePort.exportResults(results);
//
//        } catch (IOException e) {
//            log.error(e.getMessage(), e);
//
//        } finally {
//            executor.shutdown();
//        }
//    }
//
//    private Optional<EvaluationOutput> processSingleCase(EvaluationCase testCase) {
//        try {
//            log.info("케이스 평가 시작 - id: {}", testCase.getRecoveryId());
//
//            EvaluationResult zeroShot = evaluateRecoveryPort.evaluateZeroShot(testCase);
//
//            List<RetrievedCase> similarCases = retrieveReferenceCasePort.retrieve(testCase, 5);
//
//            EvaluationResult rag = evaluateRecoveryPort.evaluateWithRag(testCase, similarCases);
//
//            return Optional.of(EvaluationOutput.from(
//                    testCase,
//                    zeroShot,
//                    rag,
//                    similarCases
//            ));
//
//        } catch (Exception e) {
//            log.error("케이스 평가 실패 - id: {}, 사유: {}", testCase.getRecoveryId(), e.getMessage());
//            return Optional.empty();
//        }
//    }
//}
//
