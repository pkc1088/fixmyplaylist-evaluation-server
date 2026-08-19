package evaluation.evaluationService.evaluation.adapter.out.csv;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import evaluation.evaluationService.evaluation.application.port.out.CsvPort;
import evaluation.evaluationService.evaluation.domain.model.ReferenceCase;
import evaluation.evaluationService.evaluation.domain.model.enums.EvaluationLabel;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RequiredArgsConstructor
public class CsvReaderAdapter implements CsvPort {

    private final String bucketName;
    private final String pathName;


    @Override
    public List<ReferenceCase> read() throws IOException, CsvException {

        try (Reader reader = createReader();
             CSVReader csvReader = new CSVReader(reader)) {

            return csvReader.readAll().stream()
                    .skip(1)
                    .map(this::toCase)
                    .toList();
        }
    }

    private Reader createReader() throws IOException {
        if (StringUtils.hasText(bucketName)) {

            Storage storage = StorageOptions.getDefaultInstance().getService();
            Blob blob = storage.get(bucketName, pathName);
            if (blob == null) {
                throw new IOException("GCS에서 CSV를 찾을 수 없습니다: " + pathName);
            }
            return new InputStreamReader(Channels.newInputStream(blob.reader()), StandardCharsets.UTF_8);

        } else {
            ClassPathResource resource = new ClassPathResource(pathName);
            return new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8);
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