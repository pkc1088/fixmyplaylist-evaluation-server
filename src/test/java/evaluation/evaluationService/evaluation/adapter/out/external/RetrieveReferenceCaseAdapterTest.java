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

import static org.junit.jupiter.api.Assertions.*;
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
        ReferenceCase reference1 = ReferenceCase.create(
                "reference-1", "Target song 1", "Source song 1", EvaluationLabel.SUCCESS, "human reason 1");
        ReferenceCase reference2 = ReferenceCase.create(
                "reference-2", "Target song 2", "Source song 2", EvaluationLabel.FAIL, "human reason 2");
        Embedding embedding1 = Embedding.from(new float[]{0.1f, 0.2f});
        Embedding embedding2 = Embedding.from(new float[]{0.3f, 0.4f});

        when(embeddingModel.embedAll(anyList())).thenReturn(new Response<>(List.of(embedding1, embedding2)));

        adapter.index(List.of(reference1, reference2));

        verify(embeddingStore).addAll(idCaptor.capture(), embeddingCaptor.capture(), segmentCaptor.capture());

        String expectedId1 = UUID.nameUUIDFromBytes("reference-1".getBytes(StandardCharsets.UTF_8)).toString();
        String expectedId2 = UUID.nameUUIDFromBytes("reference-2".getBytes(StandardCharsets.UTF_8)).toString();
        assertEquals(List.of(expectedId1, expectedId2), idCaptor.getValue());
        assertEquals(List.of(embedding1, embedding2), embeddingCaptor.getValue());
        assertEquals("Target: Target song 1\nSource: Source song 1\n", segmentCaptor.getValue().get(0).text());
        assertEquals("reference-1", segmentCaptor.getValue().get(0).metadata().getString("referenceCaseId"));
        assertEquals("Target: Target song 2\nSource: Source song 2\n", segmentCaptor.getValue().get(1).text());
        assertEquals("reference-2", segmentCaptor.getValue().get(1).metadata().getString("referenceCaseId"));
    }

    @Test
    void indexSkipsExternalCallsWhenInputIsNullOrEmpty() {
        adapter.index(null);
        adapter.index(List.of());

        verifyNoInteractions(embeddingModel, embeddingStore);
    }

    @Test
    void retrieveEmbedsQueryAndSearchesWithoutFilterOrMinScore() {
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
        EmbeddingSearchRequest captured = searchRequestCaptor.getValue();
        assertEquals(queryEmbedding, captured.queryEmbedding());
        assertEquals(5, captured.maxResults());
        assertNull(captured.filter());
        assertEquals(0.0, captured.minScore());
    }

    @Test
    void retrieveIgnoresMatchesWithNullSegmentOrMissingOrBlankReferenceCaseId() {
        EvaluationCase evaluation = EvaluationCase.createPending("event-2", "Target song", "Source song");
        Embedding queryEmbedding = Embedding.from(new float[]{0.5f, 0.6f});

        when(embeddingModel.embed("Target: Target song\nSource: Source song\n"))
                .thenReturn(new Response<>(queryEmbedding));

        TextSegment validSegment = TextSegment.from("valid");
        validSegment.metadata().put("referenceCaseId", "reference-3");
        TextSegment missingMetadataSegment = TextSegment.from("orphan");
        TextSegment blankMetadataSegment = TextSegment.from("blank");
        blankMetadataSegment.metadata().put("referenceCaseId", "   ");

        when(embeddingStore.search(any(EmbeddingSearchRequest.class)))
                .thenReturn(new EmbeddingSearchResult<>(List.of(
                        new EmbeddingMatch<>(0.90, "point-1", null, validSegment),
                        new EmbeddingMatch<>(0.88, "point-null", null, null),
                        new EmbeddingMatch<>(0.85, "point-2", null, missingMetadataSegment),
                        new EmbeddingMatch<>(0.80, "point-3", null, blankMetadataSegment))));

        List<VectorSearchResult> results = adapter.retrieveIds(evaluation, 5);

        assertEquals(List.of(new VectorSearchResult("reference-3", 0.90)), results);
    }

    @Test
    void propagatesEmbeddingAndStoreExceptions() {
        EvaluationCase evaluation = EvaluationCase.createPending("event-err", "Target", "Source");
        when(embeddingModel.embed(anyString())).thenThrow(new IllegalStateException("embed failed"));

        assertThrows(IllegalStateException.class, () -> adapter.retrieveIds(evaluation, 5));
        verifyNoInteractions(embeddingStore);
    }
}
