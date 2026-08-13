package evaluation.evaluationService.evaluation.application.service;

import com.opencsv.exceptions.CsvException;
import evaluation.evaluationService.evaluation.adapter.out.csv.RecoveryCaseCsvReader;
import evaluation.evaluationService.evaluation.application.port.in.ReferenceCaseSetupUseCase;
import evaluation.evaluationService.evaluation.application.port.out.RetrieveReferenceCasePort;
import evaluation.evaluationService.evaluation.application.port.out.reference.CommandReferenceCasePort;
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

    // EvaluationRunnerService 참고

    private final RetrieveReferenceCasePort retrieveReferenceCasePort;
    private final CommandReferenceCasePort commandReferenceCasePort;
    private final RecoveryCaseCsvReader recoveryCaseCsvReader; // <- Port 로 만들기

    @Override
    public void loadCsvAndInitialize() {

        // 1. 트랜잭션이 걸린 내부 메서드를 호출하여 DB에 안전하게 저장 (commit 됨)
        List<ReferenceCase> csvReferenceData;
        try {
            csvReferenceData = new ArrayList<>(recoveryCaseCsvReader.read());

        } catch(IOException | CsvException e) {
            log.error(e.getMessage(), e);
            return;
        }
        commandReferenceCasePort.saveAll(csvReferenceData);

        // 2. 트랜잭션이 끝난 후 Qdrant 외부 API 호출 (시간 오래 걸림)
        retrieveReferenceCasePort.index(csvReferenceData);
    }

    @Override
    public void reindexAllFromDatabase() {

    }
}
