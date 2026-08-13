package evaluation.evaluationService.evaluation.infrastructure.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QdrantConfig {

    @Value("${qdrant.host}")
    private String qdrantHost;

    @Value("${qdrant.port}")
    private int qdrantPort;

    @Value("${qdrant.api-key}")
    private String qdrantApiKey;

    @Value("${qdrant.use-tls}")
    private boolean useTls;

    @Value("${qdrant.collection-name}")
    private String collectionName;

    @Bean
    public EmbeddingStore<TextSegment> qdrantEmbeddingStore() {
        return QdrantEmbeddingStore.builder()
                .host(qdrantHost)
                .port(qdrantPort)
                .apiKey(qdrantApiKey)
                .useTls(useTls)
                .collectionName(collectionName)
                .build();
    }
}