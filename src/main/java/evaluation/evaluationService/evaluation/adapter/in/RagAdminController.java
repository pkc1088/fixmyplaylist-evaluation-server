package evaluation.evaluationService.evaluation.adapter.in;

import evaluation.evaluationService.evaluation.application.port.in.EvaluationCaseUseCase;
import evaluation.evaluationService.evaluation.application.port.in.ReferenceCaseUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/rag")
public class RagAdminController {

    private final ReferenceCaseUseCase referenceCaseUseCase;
    private final EvaluationCaseUseCase evaluationCaseUseCase;


    // 관리자 메뉴얼: CSV 읽고 CloudSQL 과 Qdrant 양쪽에 초기 셋업
    @PostMapping("/setup/initial")
    public ResponseEntity<String> setupInitialData() {
        referenceCaseUseCase.loadCsvAndInitialize();
        return ResponseEntity.ok("CSV 데이터 기반 초기 세팅 완료");
    }

    // 관리자 메뉴얼: 실패 대응, 모델 교체 대비, 기존 CloudSQL 에 있는 데이터만 읽어서 Qdrant 에 새로 임베딩해서 덮어씀
    @PostMapping("/setup/reindex")
    public ResponseEntity<String> reindexAllVectors() {
        referenceCaseUseCase.reindexAllFromDatabase();
        return ResponseEntity.ok("Qdrant 전체 재색인 완료");
    }

    // 자동화: 신규 데이터 혹은 장애 이후의 PENDING 데이터들 평가
    @PostMapping("/evaluate/pending")
    public ResponseEntity<String> evaluatePendingCases() {
        evaluationCaseUseCase.evaluatePendingCases();
        return ResponseEntity.ok("PENDING 상태 케이스 일괄 평가 완료");
    }

    /*
    // 추후 논의
    @PostMapping("/promote/{evaluationCaseId}")
    public ResponseEntity<String> promoteToReference(
            @PathVariable String evaluationCaseId,
            @RequestBody PromotionRequest request
    ) {
        EvaluationCase evaluationCase = queryEvaluationCasePort.findById(evaluationCaseId);

        promotionService.promote(
                evaluationCaseId,
                evaluationCase,
                request.humanLabel(),
                request.humanReason()
        );
        return ResponseEntity.ok("레퍼런스 승격 및 색인 완료");
    }
    */
}
