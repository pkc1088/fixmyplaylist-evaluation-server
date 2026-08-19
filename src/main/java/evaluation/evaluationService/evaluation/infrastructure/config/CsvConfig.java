package evaluation.evaluationService.evaluation.infrastructure.config;

import evaluation.evaluationService.evaluation.adapter.out.csv.CsvReaderAdapter;
import evaluation.evaluationService.evaluation.application.port.out.CsvPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class CsvConfig {

    @Bean
    public CsvPort csvReadConfiguration(
            @Value("${app.csv.bucket:}") String bucketName,
            @Value("${app.csv.path:data/recovery_cases.csv}") String pathName
    ) {
        return new CsvReaderAdapter(bucketName, pathName);
    }
}
