package evaluation.evaluationService.evaluation.adapter.in;

import evaluation.evaluationService.evaluation.application.port.in.ReferenceCaseSetupUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/rag")
@RequiredArgsConstructor
public class RagAdminController {

    private final ReferenceCaseSetupUseCase referenceCaseSetupUseCase;


    // 초기 셋업: CSV 읽고 CloudSQL 과 Qdrant 양쪽에 모두 붓는 작업
    @PostMapping("/setup/initial")
    public ResponseEntity<String> setupInitialData() {
        referenceCaseSetupUseCase.loadCsvAndInitialize();
        return ResponseEntity.ok("CSV 데이터 기반 초기 세팅 완료");
    }

    // 모델 교체 대비: 기존 CloudSQL 에 있는 데이터만 읽어서 Qdrant 에 새로 임베딩해서 덮어쓰는 작업
    @PostMapping("/setup/reindex")
    public ResponseEntity<String> reindexAllVectors() {
        referenceCaseSetupUseCase.reindexAllFromDatabase();
        return ResponseEntity.ok("Qdrant 전체 재색인 완료");
    }

    // 단건 수동 삭제/추가 등 테스트용 엔드포인트...
}
