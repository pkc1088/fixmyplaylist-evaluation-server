package evaluation.evaluationService.evaluation.adapter.in;

import evaluation.evaluationService.evaluation.application.port.in.ReferenceCaseSetupUseCase;
import evaluation.evaluationService.evaluation.application.port.in.RecoveryEvaluationUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/rag")
public class RagAdminController {

    private final ReferenceCaseSetupUseCase referenceCaseSetupUseCase;
    private final RecoveryEvaluationUseCase recoveryEvaluationUseCase;


    // 초기 셋업: CSV 읽고 CloudSQL 과 Qdrant 양쪽에 모두 붓는 작업
    @PostMapping("/setup/initial")
    public ResponseEntity<String> setupInitialData(@RequestParam(required = false) Integer limit) {
        referenceCaseSetupUseCase.loadCsvAndInitialize(limit);
        return ResponseEntity.ok("CSV 데이터 기반 초기 세팅 완료");
    }

    // 모델 교체 대비: 기존 CloudSQL 에 있는 데이터만 읽어서 Qdrant 에 새로 임베딩해서 덮어쓰는 작업
    @PostMapping("/setup/reindex")
    public ResponseEntity<String> reindexAllVectors() {
        referenceCaseSetupUseCase.reindexAllFromDatabase();
        return ResponseEntity.ok("Qdrant 전체 재색인 완료");
    }

    // Kafka 컨슘 실패나 장애로 인해 PENDING 상태로 멈춰있는 데이터들을 수동으로 재처리 (배치 대용)
    @PostMapping("/evaluate/pending")
    public ResponseEntity<String> evaluatePendingCases() {
        recoveryEvaluationUseCase.evaluatePendingCases();
        return ResponseEntity.ok("PENDING 상태 케이스 일괄 평가 완료");
    }

    /*
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
