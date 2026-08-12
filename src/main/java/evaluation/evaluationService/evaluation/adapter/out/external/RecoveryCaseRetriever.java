package evaluation.evaluationService.evaluation.adapter.out.external;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import evaluation.evaluationService.evaluation.application.port.out.reference.QueryReferenceCasePort;
import evaluation.evaluationService.evaluation.application.port.out.RetrieveReferenceCasePort;
import evaluation.evaluationService.evaluation.domain.model.EvaluationCase;
import evaluation.evaluationService.evaluation.domain.model.ReferenceCase;
import evaluation.evaluationService.evaluation.domain.model.vo.RetrievedCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RecoveryCaseRetriever implements RetrieveReferenceCasePort {

    private final QueryReferenceCasePort queryReferenceCasePort; // <- 상위에서 주자
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;


    @Override
    public void index(List<ReferenceCase> cases) {
        List<TextSegment> segments = cases.stream()
                .map(this::toEmbeddingSegment)
                .toList();

        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();

        embeddingStore.addAll(embeddings, segments);
    }

    @Override
    public List<RetrievedCase> retrieve(EvaluationCase testCase, int topK) {

        String queryText = toEmbeddingText(testCase.getTargetTitle(), testCase.getSourceTitle());

        Embedding queryEmbedding = embeddingModel
                .embed(queryText)
                .content();

        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(topK)
                .build();

        // EmbeddingSearchResult<TextSegment> result = embeddingStore.search(request);
        List<EmbeddingMatch<TextSegment>> matches = embeddingStore.search(request).matches();

        List<String> caseIds = matches.stream()
                .map(m -> m.embedded().metadata().getString("referenceCaseId"))
                .toList();

        Map<String, ReferenceCase> caseById = queryReferenceCasePort.loadByIds(caseIds).stream()
                .collect(Collectors.toMap(ReferenceCase::getReferenceCaseId, Function.identity()));

        return matches.stream()
                .map(match -> {
                    String caseId = match.embedded().metadata().getString("referenceCaseId");
                    ReferenceCase referenceCase = caseById.get(caseId);
                    if (referenceCase == null) {
                        return null; // Qdrant 엔 있는데 CloudSQL 엔 없는 상태 = 데이터 불일치, 로깅 후 스킵
                    }
                    return new RetrievedCase(referenceCase, match.score());
                })
                .filter(Objects::nonNull)
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