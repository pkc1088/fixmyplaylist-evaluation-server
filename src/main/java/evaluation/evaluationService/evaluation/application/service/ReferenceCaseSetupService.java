package evaluation.evaluationService.evaluation.application.service;

import com.opencsv.exceptions.CsvException;
import evaluation.evaluationService.evaluation.application.port.in.ReferenceCaseUseCase;
import evaluation.evaluationService.evaluation.application.port.out.CsvPort;
import evaluation.evaluationService.evaluation.application.port.out.RetrieveReferenceCasePort;
import evaluation.evaluationService.evaluation.application.port.out.CommandReferenceCasePort;
import evaluation.evaluationService.evaluation.application.port.out.QueryReferenceCasePort;
import evaluation.evaluationService.evaluation.domain.exception.ReferenceCaseSetupException;
import evaluation.evaluationService.evaluation.domain.model.ReferenceCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReferenceCaseSetupService implements ReferenceCaseUseCase {

    private final RetrieveReferenceCasePort retrieveReferenceCasePort;
    private final CommandReferenceCasePort commandReferenceCasePort;
    private final QueryReferenceCasePort queryReferenceCasePort;
    private final CsvPort csvPort;


    @Override
    public void loadCsvAndInitialize() {

        List<ReferenceCase> csvReferenceData;
        try {
            csvReferenceData = new ArrayList<>(csvPort.read());

        } catch(IOException | CsvException e) {
            throw new ReferenceCaseSetupException("CSV ReferenceData Read Exception", e);
        }

        commandReferenceCasePort.saveAll(csvReferenceData); // CloudSQL/MySQL 적재
        retrieveReferenceCasePort.index(csvReferenceData);  // Qdrant 적재
    }

    @Override
    public void reindexAllFromDatabase() {
        List<ReferenceCase> all = queryReferenceCasePort.loadAll();
        retrieveReferenceCasePort.index(all);
        log.info("전체 재색인 완료: {}건", all.size());
    }
}
