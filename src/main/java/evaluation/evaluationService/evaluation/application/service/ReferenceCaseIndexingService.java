package evaluation.evaluationService.evaluation.application.service;

import evaluation.evaluationService.evaluation.application.port.out.RetrieveReferenceCasePort;
import evaluation.evaluationService.evaluation.application.port.out.reference.CommandReferenceCasePort;
import evaluation.evaluationService.evaluation.domain.model.ReferenceCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReferenceCaseIndexingService {

    private final RetrieveReferenceCasePort retrieveReferenceCasePort;
    private final CommandReferenceCasePort commandReferenceCasePort;


    // @Transactional ?
    public void indexNewCases(List<ReferenceCase> cases) {
        commandReferenceCasePort.saveAll(cases);    // 1. CloudSQL: source of truth
        retrieveReferenceCasePort.index(cases);     // 2. Qdrant: 검색 인덱스
    }
}