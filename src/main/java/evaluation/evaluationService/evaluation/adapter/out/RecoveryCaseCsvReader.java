package evaluation.evaluationService.evaluation.adapter.out;

import com.opencsv.exceptions.CsvException;
import evaluation.evaluationService.evaluation.application.port.out.LoadRecoveryCasePort;
import evaluation.evaluationService.evaluation.domain.model.enums.HumanLabel;
import evaluation.evaluationService.evaluation.domain.model.vo.RecoveryCase;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import com.opencsv.CSVReader;

@Component
public class RecoveryCaseCsvReader implements LoadRecoveryCasePort {

    public List<RecoveryCase> read() throws IOException, CsvException {

        Path inputPath = Path.of("data/recovery_cases.csv");

        try (Reader reader = Files.newBufferedReader(inputPath, StandardCharsets.UTF_8);
            CSVReader csvReader = new CSVReader(reader)) {

            List<String[]> rows = csvReader.readAll();

            return rows.stream()
                    .skip(1)
                    .map(this::toCase)
                    .toList();
        }
    }

    private RecoveryCase toCase(String[] row) {
        return new RecoveryCase(
                row[0],
                row[1],
                row[2],
                HumanLabel.valueOf(row[3]),
                row[4]
        );
    }
}