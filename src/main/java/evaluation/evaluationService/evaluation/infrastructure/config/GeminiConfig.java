package evaluation.evaluationService.evaluation.infrastructure.config;

import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

import java.time.Duration;

@Configuration
public class GeminiConfig {

    @Bean
    public EmbeddingModel embeddingModel(
            @Value("${gemini.api-key}") String apiKey
    ) {
        return GoogleAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .modelName("gemini-embedding-001")
                .outputDimensionality(768)
                .timeout(Duration.ofSeconds(30))
                .taskType(GoogleAiEmbeddingModel.TaskType.SEMANTIC_SIMILARITY)
                .build();
    }

    @Bean
    public ChatModel chatModel(
            @Value("${gemini.api-key}") String apiKey
    ) {
        return GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gemini-2.5-flash-lite")
                .temperature(0.0)
                .maxOutputTokens(256)
                .timeout(Duration.ofSeconds(30))
                .responseFormat(ResponseFormat.JSON)
                .supportedCapabilities(Capability.RESPONSE_FORMAT_JSON_SCHEMA)
                //.logRequestsAndResponses(true)
                .build();
    }
}