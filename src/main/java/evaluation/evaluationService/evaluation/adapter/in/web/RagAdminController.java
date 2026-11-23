package evaluation.evaluationService.evaluation.adapter.in.web;

import evaluation.evaluationService.evaluation.application.port.in.EvaluationCaseUseCase;
import evaluation.evaluationService.evaluation.application.port.in.PromotionUseCase;
import evaluation.evaluationService.evaluation.application.port.in.ReferenceCaseUseCase;
import evaluation.evaluationService.evaluation.application.port.in.dto.IndexRetryResult;
import evaluation.evaluationService.evaluation.adapter.in.web.dto.PromotionRequest;
import evaluation.evaluationService.evaluation.application.port.in.dto.ReviewCaseResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/rag")
public class RagAdminController {

    private final ReferenceCaseUseCase referenceCaseUseCase;
    private final EvaluationCaseUseCase evaluationCaseUseCase;
    private final PromotionUseCase promotionUseCase;


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

    // 관리자 메뉴얼: AI_REVIEWED -> HUMAN_REVIEWED 로 승진
    @GetMapping("/review/pending")
    public ResponseEntity<List<ReviewCaseResponse>> loadPendingReviewCases() {
        List<ReviewCaseResponse> cases = evaluationCaseUseCase.loadPendingReviewCases();
        return ResponseEntity.ok(cases);
    }

    @PostMapping("/promote/{evaluationCaseId}")
    public ResponseEntity<IndexRetryResult> promoteToReference(
            @PathVariable String evaluationCaseId,
            @Valid @RequestBody PromotionRequest request
    ) {
        return ResponseEntity.ok(promotionUseCase.promote(
                evaluationCaseId,
                request.humanLabel(),
                request.humanReason())
        );
    }

    @PostMapping("/promotion/indexing/retry")
    public ResponseEntity<IndexRetryResult> retryPendingIndexing() {
        return ResponseEntity.ok(promotionUseCase.retryPendingIndexing());
    }
}
