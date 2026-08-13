package evaluation.evaluationService.evaluation.adapter.out.external;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import evaluation.evaluationService.evaluation.application.port.out.RetrieveReferenceCasePort;
import evaluation.evaluationService.evaluation.application.port.out.dto.VectorSearchResult;
import evaluation.evaluationService.evaluation.domain.model.EvaluationCase;
import evaluation.evaluationService.evaluation.domain.model.ReferenceCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RetrieveReferenceCaseAdapter implements RetrieveReferenceCasePort {

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;


    @Override
    public void index(List<ReferenceCase> cases) {
        List<TextSegment> segments = cases.stream()
                .map(this::toEmbeddingSegment)
                .toList();

        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();

        List<String> deterministicIds = cases.stream()
                .map(c -> UUID.nameUUIDFromBytes(c.getReferenceCaseId().getBytes(StandardCharsets.UTF_8)).toString())
                .toList();

        embeddingStore.addAll(deterministicIds, embeddings, segments);
    }

    public List<VectorSearchResult> retrieveIds(EvaluationCase testCase, int topK) {

        String queryText = toEmbeddingText(testCase.getTargetTitle(), testCase.getSourceTitle());

        Embedding queryEmbedding = embeddingModel
                .embed(queryText)
                .content();

        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(topK)
                .build();

        List<EmbeddingMatch<TextSegment>> matches = embeddingStore.search(request).matches();

        return matches.stream()
                .map(m -> new VectorSearchResult(
                        m.embedded().metadata().getString("referenceCaseId"),
                        m.score()))
                .toList();
    }

    private TextSegment toEmbeddingSegment(ReferenceCase referenceCase) {
        Metadata metadata = new Metadata();
        metadata.put("referenceCaseId", referenceCase.getReferenceCaseId()); // Qdrant Point Payload 에 caseId 저장

        return TextSegment.from(
                toEmbeddingText(referenceCase.getTargetTitle(), referenceCase.getSourceTitle()),
                metadata
        );
    }

    private String toEmbeddingText(String targetTitle, String sourceTitle) {
        return """
                Target: %s
                Source: %s
                """.formatted(targetTitle, sourceTitle);
    }
}