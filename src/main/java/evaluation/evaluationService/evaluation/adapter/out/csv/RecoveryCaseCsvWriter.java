//package evaluation.evaluationService.evaluation.adapter.out.csv;
//
//import com.opencsv.CSVWriter;
//import evaluation.evaluationService.evaluation.application.port.out.SaveEvaluationCasePort;
//import evaluation.evaluationService.evaluation.domain.model.vo.EvaluationOutput;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Component;
//
//import java.io.IOException;
//import java.io.Writer;
//import java.nio.charset.StandardCharsets;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.StandardOpenOption;
//import java.util.List;
//
//@Slf4j
//@Component
//public class RecoveryCaseCsvWriter implements SaveEvaluationCasePort {
//
//    public void exportResults(List<EvaluationOutput> results) throws IOException {
//
//        Path outputPath = Path.of("data/evaluation_results.csv");
//        boolean fileExistsAndNotEmpty = Files.exists(outputPath) && Files.size(outputPath) > 0;
//
//        try (
//                Writer writer = Files.newBufferedWriter(
//                        outputPath,
//                        StandardCharsets.UTF_8,
//                        StandardOpenOption.CREATE,
//                        StandardOpenOption.APPEND
//                );
//                CSVWriter csvWriter = new CSVWriter(writer)
//        ) {
//            if (!fileExistsAndNotEmpty) {
//                csvWriter.writeNext(new String[]{
//                        "id",
//                        "human_label",
//
//                        "zero_shot_label",
//                        "zero_shot_confidence",
//                        "zero_shot_correct",
//
//                        "rag_label",
//                        "rag_confidence",
//                        "rag_correct",
//
//                        "retrieved_case_ids"
//                });
//            }
//
//            for (EvaluationOutput result : results) {
//
//                csvWriter.writeNext(new String[]{
//                        result.id(),
//                        result.humanLabel().name(),
//                        result.zeroShotLabel().name(),
//                        String.valueOf(result.zeroShotConfidence()),
//                        String.valueOf(result.humanLabel() == result.zeroShotLabel()),
//
//                        result.ragLabel().name(),
//                        String.valueOf(result.ragConfidence()),
//                        String.valueOf(result.humanLabel() == result.ragLabel()),
//
//                        String.join("|", result.retrievedCaseInfo())
//                });
//            }
//        }
//
//        log.info("Evaluation results exported to {}", outputPath);
//    }
//}
