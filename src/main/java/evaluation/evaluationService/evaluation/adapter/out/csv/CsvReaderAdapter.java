package evaluation.evaluationService.evaluation.adapter.out.csv;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import evaluation.evaluationService.evaluation.application.port.out.CsvPort;
import evaluation.evaluationService.evaluation.domain.model.ReferenceCase;
import evaluation.evaluationService.evaluation.domain.model.enums.EvaluationLabel;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class CsvReaderAdapter implements CsvPort {

    private static final String CSV_PATH = "data/recovery_cases.csv";


    @Override
    public List<ReferenceCase> read() throws IOException, CsvException {

        ClassPathResource resource = new ClassPathResource(CSV_PATH);

        try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8);
             CSVReader csvReader = new CSVReader(reader)) {

            return csvReader.readAll().stream()
                    .skip(1)
                    .map(this::toCase)
                    .toList();
        }
    }

    private ReferenceCase toCase(String[] row) {
        return ReferenceCase.create(
                row[0],
                row[2], // target_video_title (원본 비정상 영상)
                row[1], // source_video_title (실제 복구된 영상)
                EvaluationLabel.valueOf(row[3]),
                row[4]
        );
    }
}