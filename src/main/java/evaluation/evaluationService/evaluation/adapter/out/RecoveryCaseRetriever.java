package evaluation.evaluationService.evaluation.adapter.out;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import evaluation.evaluationService.evaluation.application.port.out.RetrieveReferenceCasePort;
import evaluation.evaluationService.evaluation.domain.model.vo.RecoveryCase;
import evaluation.evaluationService.evaluation.domain.model.vo.RetrievedCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RecoveryCaseRetriever implements RetrieveReferenceCasePort {

    private final List<RecoveryCase> referenceCases = new ArrayList<>();
    private final InMemoryEmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;


    public void index(List<RecoveryCase> cases) {
        referenceCases.addAll(cases);

        List<TextSegment> segments = cases.stream()
                .map(this::toEmbeddingSegment)
                .toList();

        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();

        embeddingStore.addAll(embeddings, segments);
    }

    public List<RetrievedCase> retrieve(RecoveryCase testCase, int topK) {

        Embedding queryEmbedding = embeddingModel
                .embed(toEmbeddingText(testCase))
                .content();

        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(topK)
                .build();

        EmbeddingSearchResult<TextSegment> result = embeddingStore.search(request);

        return result.matches().stream()
                .map(match -> new RetrievedCase(
                        findReferenceCase(match.embedded().metadata().getString("caseId")),
                        match.score()
                ))
                .toList();
    }

    private TextSegment toEmbeddingSegment(RecoveryCase recoveryCase) {
        Metadata metadata = new Metadata();
        metadata.put("caseId", recoveryCase.id());

        return TextSegment.from(
                toEmbeddingText(recoveryCase),
                metadata
        );
    }

    private String toEmbeddingText(RecoveryCase recoveryCase) {
        return """
                Target: %s
                Source: %s
                """.formatted(
                recoveryCase.targetTitle(),
                recoveryCase.sourceTitle()
        );
    }

    private RecoveryCase findReferenceCase(String caseId) {
        return referenceCases.stream()
                .filter(c -> c.id().equals(caseId))
                .findFirst()
                .orElseThrow();
    }
}