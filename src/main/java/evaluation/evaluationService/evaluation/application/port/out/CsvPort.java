package evaluation.evaluationService.evaluation.application.port.out;

import com.opencsv.exceptions.CsvException;
import evaluation.evaluationService.evaluation.domain.model.ReferenceCase;

import java.io.IOException;
import java.util.List;

public interface CsvPort {

    List<ReferenceCase> read(Integer limit) throws IOException, CsvException;
}
