package evaluation.evaluationService.evaluation.adapter.out.external;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import evaluation.evaluationService.evaluation.application.port.out.dto.EvaluationResult;
import evaluation.evaluationService.evaluation.domain.model.EvaluationCase;
import evaluation.evaluationService.evaluation.domain.model.ReferenceCase;
import evaluation.evaluationService.evaluation.domain.model.enums.EvaluationLabel;
import evaluation.evaluationService.evaluation.domain.model.vo.RetrievedCase;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class GeminiEvaluatorAdapterTest {

    @Mock private ChatModel chatModel;
    @InjectMocks private GeminiEvaluatorAdapter adapter;
    @Captor private ArgumentCaptor<ChatRequest> chatRequestCaptor;

    @Test
    void parsesFixedJsonAndSendsCurrentCaseAndHumanReviewedExamples() {
        when(chatModel.chat(any(ChatRequest.class)))
                .thenReturn(ChatResponse.builder()
                        .aiMessage(AiMessage.from("{\"label\":\"SUCCESS\",\"confidence\":0.93}"))
                        .build());

        EvaluationCase evaluationCase = EvaluationCase.createPending("event-1", "Current target", "Current source");
        ReferenceCase referenceCase = ReferenceCase.create("reference-1", "Example target", "Example source", EvaluationLabel.FAIL, "Human reason");

        EvaluationResult result = adapter.evaluateWithRag(evaluationCase, List.of(new RetrievedCase(referenceCase, 0.9)));

        assertEquals(new EvaluationResult(EvaluationLabel.SUCCESS, 0.93), result);
        verify(chatModel).chat(chatRequestCaptor.capture());

        String prompt = extractPromptText(chatRequestCaptor.getValue());
        assertTrue(prompt.contains("Current target"));
        assertTrue(prompt.contains("Current source"));
        assertTrue(prompt.contains("Example target"));
        assertTrue(prompt.contains("Example source"));
        assertTrue(prompt.contains("Human Label: FAIL"));
        assertTrue(prompt.contains("Human Reason: Human reason"));
    }

    @Test
    void handlesNoRetrievedExamples() {
        when(chatModel.chat(any(ChatRequest.class)))
                .thenReturn(ChatResponse.builder()
                        .aiMessage(AiMessage.from("{\"label\":\"FAIL\",\"confidence\":0.4}"))
                        .build());

        EvaluationResult result = adapter.evaluateWithRag(EvaluationCase.createPending("event-2", "Target", "Source"), List.of());

        assertEquals(new EvaluationResult(EvaluationLabel.FAIL, 0.4), result);
        verify(chatModel).chat(chatRequestCaptor.capture());

        String prompt = extractPromptText(chatRequestCaptor.getValue());
        assertTrue(prompt.contains("Target"));
        assertTrue(prompt.contains("Source"));
    }

    @Test
    void throwsWhenModelReturnsMalformedJson() {
        when(chatModel.chat(any(ChatRequest.class)))
                .thenReturn(ChatResponse.builder()
                        .aiMessage(AiMessage.from("not-valid-json"))
                        .build());

        EvaluationCase evaluationCase = EvaluationCase.createPending("event-3", "Target", "Source");

        assertThrows(RuntimeException.class, () -> adapter.evaluateWithRag(evaluationCase, List.of()));
    }

    @Test
    void throwsWhenModelReturnsUnknownLabel() {
        when(chatModel.chat(any(ChatRequest.class)))
                .thenReturn(ChatResponse.builder()
                        .aiMessage(AiMessage.from("{\"label\":\"UNKNOWN\",\"confidence\":0.5}"))
                        .build());

        EvaluationCase evaluationCase = EvaluationCase.createPending("event-4", "Target", "Source");

        assertThrows(RuntimeException.class, () -> adapter.evaluateWithRag(evaluationCase, List.of()));
    }

    private String extractPromptText(ChatRequest request) {
        return request.messages().stream()
                .map(this::extractMessageText)
                .collect(Collectors.joining("\n"));
    }

    private String extractMessageText(ChatMessage message) {
        if (message instanceof SystemMessage systemMessage) {
            return systemMessage.text();
        }
        if (message instanceof UserMessage userMessage) {
            return userMessage.contents().stream()
                    .filter(TextContent.class::isInstance)
                    .map(content -> ((TextContent) content).text())
                    .collect(Collectors.joining("\n"));
        }
        return "";
    }
}