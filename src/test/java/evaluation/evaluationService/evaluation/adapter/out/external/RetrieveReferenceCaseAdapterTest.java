package evaluation.evaluationService.evaluation.adapter.out.external;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import evaluation.evaluationService.evaluation.application.port.out.dto.VectorSearchResult;
import evaluation.evaluationService.evaluation.domain.model.EvaluationCase;
import evaluation.evaluationService.evaluation.domain.model.ReferenceCase;
import evaluation.evaluationService.evaluation.domain.model.enums.EvaluationLabel;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class RetrieveReferenceCaseAdapterTest {

    @Mock private EmbeddingStore<TextSegment> embeddingStore;
    @Mock private EmbeddingModel embeddingModel;
    @InjectMocks private RetrieveReferenceCaseAdapter adapter;

    @Captor private ArgumentCaptor<List<String>> idCaptor;
    @Captor private ArgumentCaptor<List<Embedding>> embeddingCaptor;
    @Captor private ArgumentCaptor<List<TextSegment>> segmentCaptor;
    @Captor private ArgumentCaptor<EmbeddingSearchRequest> searchRequestCaptor;

    @Test
    void indexEmbedsTargetAndSourceAndUsesStableReferenceId() {
        ReferenceCase reference = ReferenceCase.create(
                "reference-1", "Target song", "Source song", EvaluationLabel.SUCCESS, "human reason");
        Embedding embedding = Embedding.from(new float[]{0.1f, 0.2f});

        when(embeddingModel.embedAll(anyList())).thenReturn(new Response<>(List.of(embedding)));

        adapter.index(List.of(reference));

        verify(embeddingStore).addAll(idCaptor.capture(), embeddingCaptor.capture(), segmentCaptor.capture());

        String expectedId = UUID.nameUUIDFromBytes("reference-1".getBytes(StandardCharsets.UTF_8)).toString();
        assertEquals(List.of(expectedId), idCaptor.getValue());
        assertEquals(List.of(embedding), embeddingCaptor.getValue());
        assertEquals("Target: Target song\nSource: Source song\n", segmentCaptor.getValue().get(0).text());
        assertEquals("reference-1", segmentCaptor.getValue().get(0).metadata().getString("referenceCaseId"));
    }

    @Test
    void indexSkipsExternalCallsWhenInputIsEmpty() {
        adapter.index(List.of());

        verifyNoInteractions(embeddingModel, embeddingStore);
    }

    @Test
    void retrieveEmbedsQueryAndMapsIdsAndScoresFromSearchMatches() {
        EvaluationCase evaluation = EvaluationCase.createPending("event-1", "Target song", "Source song");
        Embedding queryEmbedding = Embedding.from(new float[]{0.3f, 0.4f});

        when(embeddingModel.embed("Target: Target song\nSource: Source song\n"))
                .thenReturn(new Response<>(queryEmbedding));

        TextSegment segment = TextSegment.from("example");
        segment.metadata().put("referenceCaseId", "reference-2");

        when(embeddingStore.search(any(EmbeddingSearchRequest.class)))
                .thenReturn(new EmbeddingSearchResult<>(List.of(new EmbeddingMatch<>(0.87, "point-id", null, segment))));

        List<VectorSearchResult> results = adapter.retrieveIds(evaluation, 5);

        assertEquals(List.of(new VectorSearchResult("reference-2", 0.87)), results);
        verify(embeddingStore).search(searchRequestCaptor.capture());
        assertEquals(queryEmbedding, searchRequestCaptor.getValue().queryEmbedding());
        assertEquals(5, searchRequestCaptor.getValue().maxResults());
    }

    @Test
    void retrieveIgnoresMatchesWithoutReferenceCaseIdMetadata() {
        EvaluationCase evaluation = EvaluationCase.createPending("event-2", "Target song", "Source song");
        Embedding queryEmbedding = Embedding.from(new float[]{0.5f, 0.6f});

        when(embeddingModel.embed("Target: Target song\nSource: Source song\n"))
                .thenReturn(new Response<>(queryEmbedding));

        TextSegment validSegment = TextSegment.from("valid");
        validSegment.metadata().put("referenceCaseId", "reference-3");
        TextSegment missingMetadataSegment = TextSegment.from("orphan");

        when(embeddingStore.search(any(EmbeddingSearchRequest.class)))
                .thenReturn(new EmbeddingSearchResult<>(List.of(
                        new EmbeddingMatch<>(0.90, "point-1", null, validSegment),
                        new EmbeddingMatch<>(0.85, "point-2", null, missingMetadataSegment))));

        List<VectorSearchResult> results = adapter.retrieveIds(evaluation, 5);

        assertEquals(List.of(new VectorSearchResult("reference-3", 0.90)), results);
    }
}