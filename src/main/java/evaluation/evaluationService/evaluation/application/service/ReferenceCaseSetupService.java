package evaluation.evaluationService.evaluation.application.service;

import com.opencsv.exceptions.CsvException;
import evaluation.evaluationService.evaluation.application.port.in.ReferenceCaseSetupUseCase;
import evaluation.evaluationService.evaluation.application.port.out.CsvPort;
import evaluation.evaluationService.evaluation.application.port.out.RetrieveReferenceCasePort;
import evaluation.evaluationService.evaluation.application.port.out.reference.CommandReferenceCasePort;
import evaluation.evaluationService.evaluation.application.port.out.reference.QueryReferenceCasePort;
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
public class ReferenceCaseSetupService implements ReferenceCaseSetupUseCase {

    private final ReferenceCaseIndexingService referenceCaseIndexingService;
    private final RetrieveReferenceCasePort retrieveReferenceCasePort;
    private final CommandReferenceCasePort commandReferenceCasePort;
    private final QueryReferenceCasePort queryReferenceCasePort;
    private final CsvPort csvReaderAdapter;


    @Override
    public void loadCsvAndInitialize(Integer limit) {

        List<ReferenceCase> csvReferenceData;
        try {
            csvReferenceData = new ArrayList<>(csvReaderAdapter.read(limit));

        } catch(IOException | CsvException e) {
            log.error(e.getMessage(), e);
            return; // 커스텀 ReferenceCaseSetupException 던지기
        }


        referenceCaseIndexingService.indexNewCases(csvReferenceData); // 저장+색인 로직 중복 제거
//        commandReferenceCasePort.saveAll(csvReferenceData);
//        retrieveReferenceCasePort.index(csvReferenceData);
    }

    @Override
    public void reindexAllFromDatabase() {
        List<ReferenceCase> all = queryReferenceCasePort.loadAll();
        retrieveReferenceCasePort.index(all);
        log.info("전체 재색인 완료: {}건", all.size());
    }
}
