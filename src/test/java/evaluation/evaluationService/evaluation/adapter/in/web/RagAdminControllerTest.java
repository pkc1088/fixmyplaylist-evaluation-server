package evaluation.evaluationService.evaluation.adapter.in.web;

import com.fasterxml.jackson.databind.json.JsonMapper;
import evaluation.evaluationService.evaluation.application.port.in.EvaluationCaseUseCase;
import evaluation.evaluationService.evaluation.application.port.in.PromotionUseCase;
import evaluation.evaluationService.evaluation.application.port.in.ReferenceCaseUseCase;
import evaluation.evaluationService.evaluation.application.port.in.dto.IndexRetryResult;
import evaluation.evaluationService.evaluation.application.port.in.dto.ReviewCaseResponse;
import evaluation.evaluationService.evaluation.domain.exception.EvaluationCaseNotFoundException;
import evaluation.evaluationService.evaluation.domain.exception.ReferenceCaseSetupException;
import evaluation.evaluationService.evaluation.domain.model.enums.EvaluationLabel;
import evaluation.evaluationService.evaluation.infrastructure.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class RagAdminControllerTest {

    @Mock private ReferenceCaseUseCase referenceCaseUseCase;
    @Mock private EvaluationCaseUseCase evaluationCaseUseCase;
    @Mock private PromotionUseCase promotionUseCase;
    @InjectMocks private RagAdminController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(
                        new StringHttpMessageConverter(StandardCharsets.UTF_8),
                        new MappingJackson2HttpMessageConverter(JsonMapper.builder().findAndAddModules().build()))
                .build();
    }

    @Test
    void setupInitialDelegatesToReferenceCaseUseCase() throws Exception {
        mockMvc.perform(post("/api/admin/rag/setup/initial"))
                .andExpect(status().isOk())
                .andExpect(content().string("CSV 데이터 기반 초기 세팅 완료"));

        verify(referenceCaseUseCase).loadCsvAndInitialize();
    }

    @Test
    void setupReindexDelegatesToReferenceCaseUseCase() throws Exception {
        mockMvc.perform(post("/api/admin/rag/setup/reindex"))
                .andExpect(status().isOk())
                .andExpect(content().string("Qdrant 전체 재색인 완료"));

        verify(referenceCaseUseCase).reindexAllFromDatabase();
    }

    @Test
    void evaluatePendingDelegatesToEvaluationCaseUseCase() throws Exception {
        mockMvc.perform(post("/api/admin/rag/evaluate/pending"))
                .andExpect(status().isOk())
                .andExpect(content().string("PENDING 상태 케이스 일괄 평가 완료"));

        verify(evaluationCaseUseCase).evaluatePendingCases();
    }

    @Test
    void loadPendingReviewReturnsSerializedReviewCases() throws Exception {
        LocalDateTime evaluatedAt = LocalDateTime.of(2026, 3, 1, 12, 30, 0);

        when(evaluationCaseUseCase.loadPendingReviewCases())
                .thenReturn(List.of(new ReviewCaseResponse(
                        "case-1", "Target song", "Source song", EvaluationLabel.FAIL, 0.45, evaluatedAt)));

        mockMvc.perform(get("/api/admin/rag/review/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].evaluationCaseId").value("case-1"))
                .andExpect(jsonPath("$[0].targetTitle").value("Target song"))
                .andExpect(jsonPath("$[0].sourceTitle").value("Source song"))
                .andExpect(jsonPath("$[0].aiLabel").value("FAIL"))
                .andExpect(jsonPath("$[0].aiConfidence").value(0.45));
    }

    @Test
    void promoteToReferenceDelegatesValidRequestAndReturnsCounts() throws Exception {
        when(promotionUseCase.promote("case-1", EvaluationLabel.SUCCESS, "검수 근거"))
                .thenReturn(new IndexRetryResult(1, 0));

        mockMvc.perform(post("/api/admin/rag/promote/case-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"humanLabel\":\"SUCCESS\",\"humanReason\":\"검수 근거\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.indexed").value(1))
                .andExpect(jsonPath("$.failed").value(0));
    }

    @Test
    void promoteToReferenceRejectsInvalidRequestBodyWithBadRequest() throws Exception {
        mockMvc.perform(post("/api/admin/rag/promote/case-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"humanLabel\":null,\"humanReason\":\"근거\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/admin/rag/promote/case-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"humanLabel\":\"SUCCESS\",\"humanReason\":\"   \"}"))
                .andExpect(status().isBadRequest());

        String overlongBody = "{\"humanLabel\":\"SUCCESS\",\"humanReason\":\"" + "r".repeat(4001) + "\"}";

        mockMvc.perform(post("/api/admin/rag/promote/case-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(overlongBody))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(promotionUseCase);
    }

    @Test
    void retryPendingIndexingReturnsCounts() throws Exception {
        when(promotionUseCase.retryPendingIndexing()).thenReturn(new IndexRetryResult(2, 1));

        mockMvc.perform(post("/api/admin/rag/promotion/indexing/retry"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.indexed").value(2))
                .andExpect(jsonPath("$.failed").value(1));
    }

    @Test
    void globalExceptionHandlerMapsDomainAndRuntimeExceptionsToExpectedStatuses() throws Exception {
        when(promotionUseCase.promote("missing", EvaluationLabel.SUCCESS, "근거"))
                .thenThrow(new EvaluationCaseNotFoundException("missing"));
        when(promotionUseCase.promote("conflict", EvaluationLabel.SUCCESS, "근거"))
                .thenThrow(new IllegalStateException("already reviewed"));
        when(promotionUseCase.promote("bad-arg", EvaluationLabel.SUCCESS, "근거"))
                .thenThrow(new IllegalArgumentException("invalid input"));
        doThrow(new ReferenceCaseSetupException("CSV setup failed", new RuntimeException()))
                .when(referenceCaseUseCase).loadCsvAndInitialize();

        mockMvc.perform(post("/api/admin/rag/promote/missing")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"humanLabel\":\"SUCCESS\",\"humanReason\":\"근거\"}"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("missing")));

        mockMvc.perform(post("/api/admin/rag/promote/conflict")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"humanLabel\":\"SUCCESS\",\"humanReason\":\"근거\"}"))
                .andExpect(status().isConflict())
                .andExpect(content().string("already reviewed"));

        mockMvc.perform(post("/api/admin/rag/promote/bad-arg")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"humanLabel\":\"SUCCESS\",\"humanReason\":\"근거\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("invalid input"));

        mockMvc.perform(post("/api/admin/rag/setup/initial"))
                .andExpect(status().isOk())
                .andExpect(content().string("CSV setup failed"));
    }
}
