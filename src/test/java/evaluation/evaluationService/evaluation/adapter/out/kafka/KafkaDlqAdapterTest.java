package evaluation.evaluationService.evaluation.adapter.out.kafka;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class KafkaDlqAdapterTest {

    @Mock private KafkaTemplate<String, String> kafkaTemplate;

    private KafkaDlqAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new KafkaDlqAdapter(kafkaTemplate, "dead-letter");
    }

    @Test
    void publishesOriginalMessageToConfiguredTopic() {
        when(kafkaTemplate.send("dead-letter", "raw payload"))
                .thenReturn(CompletableFuture.completedFuture(null));

        adapter.sendToDlq("raw payload");

        verify(kafkaTemplate).send("dead-letter", "raw payload");
    }

    @Test
    void throwsWhenPublishingFailsAsynchronously() {
        CompletableFuture<SendResult<String, String>> failed =
                CompletableFuture.failedFuture(new IllegalStateException("broker unavailable"));

        when(kafkaTemplate.send("dead-letter", "raw payload"))
                .thenReturn(failed);

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> adapter.sendToDlq("raw payload"));

        Throwable root = thrown;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        assertInstanceOf(IllegalStateException.class, root);
        assertEquals("broker unavailable", root.getMessage());
    }

    @Test
    void throwsWhenKafkaTemplateThrowsSynchronously() {
        IllegalStateException cause = new IllegalStateException("serialization error");
        when(kafkaTemplate.send("dead-letter", "raw payload")).thenThrow(cause);

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> adapter.sendToDlq("raw payload"));

        assertSame(cause, thrown.getCause());
    }
}
