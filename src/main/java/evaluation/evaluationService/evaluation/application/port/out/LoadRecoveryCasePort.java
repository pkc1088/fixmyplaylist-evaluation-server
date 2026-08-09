package evaluation.evaluationService.evaluation.application.port.out;

import com.opencsv.exceptions.CsvException;
import evaluation.evaluationService.evaluation.domain.model.vo.RecoveryCase;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface LoadRecoveryCasePort {

    List<RecoveryCase> read(Path path) throws IOException, CsvException;
}
