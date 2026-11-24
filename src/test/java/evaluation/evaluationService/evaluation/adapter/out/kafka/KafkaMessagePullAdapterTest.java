package evaluation.evaluationService.evaluation.adapter.out.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import evaluation.evaluationService.evaluation.application.port.out.EventProcessor;
import evaluation.evaluationService.evaluation.application.port.out.dto.RecoveryCompletedEvent;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.ConsumerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class KafkaMessagePullAdapterTest {

    @Mock private ConsumerFactory<String, String> consumerFactory;
    @Mock private Consumer<String, String> consumer;
    @Mock private EventProcessor processor;

    private KafkaMessagePullAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new KafkaMessagePullAdapter(consumerFactory, new ObjectMapper(), "events");
    }

    @Test
    void deserializesValidRecordAndCountsItAsProcessed() throws Exception {
        ConsumerRecords<String, String> records = records(
                "{\"eventId\":\"event-1\",\"targetTitle\":\"Target\",\"sourceTitle\":\"Source\"}");

        when(consumerFactory.createConsumer())
                .thenReturn(consumer);
        when(consumer.poll(any(Duration.class)))
                .thenReturn(records, empty(), empty(), empty());

        assertEquals(1, adapter.pullAndProcess(processor));

        verify(processor).process(new RecoveryCompletedEvent("event-1", "Target", "Source"));
        verify(processor, never()).onFail(any());
        verify(consumer).subscribe(List.of("events"));
        verify(consumer).commitSync();
        verify(consumer).close();
    }

    @Test
    void sendsMalformedRawRecordToFailureCallbackAndDoesNotCountIt() throws Exception {
        String raw = "{invalid-json";

        when(consumerFactory.createConsumer())
                .thenReturn(consumer);
        when(consumer.poll(any(Duration.class)))
                .thenReturn(records(raw), empty(), empty(), empty());

        assertEquals(0, adapter.pullAndProcess(processor));

        verify(processor).onFail(raw);
        verify(processor, never()).process(any());
        verify(consumer).commitSync();
        verify(consumer).close();
    }

    @Test
    void sendsRawRecordToFailureCallbackWhenProcessorThrows() throws Exception {
        String raw = "{\"eventId\":\"event-2\",\"targetTitle\":\"Target\",\"sourceTitle\":\"Source\"}";

        when(consumerFactory.createConsumer())
                .thenReturn(consumer);
        when(consumer.poll(any(Duration.class)))
                .thenReturn(records(raw), empty(), empty(), empty());
        doThrow(new IllegalStateException("db unavailable"))
                .when(processor).process(new RecoveryCompletedEvent("event-2", "Target", "Source"));

        assertEquals(0, adapter.pullAndProcess(processor));

        verify(processor).onFail(raw);
        verify(consumer).commitSync();
        verify(consumer).close();
    }

    @Test
    void stopsWithoutCommittingAndClosesConsumerWhenFailureCallbackThrows() throws Exception {
        String raw = "{invalid-json";

        when(consumerFactory.createConsumer())
                .thenReturn(consumer);
        when(consumer.poll(any(Duration.class)))
                .thenReturn(records(raw));
        doThrow(new RuntimeException("dlq failed"))
                .when(processor).onFail(raw);

        assertEquals(0, adapter.pullAndProcess(processor));

        verify(consumer, never()).commitSync();
        verify(consumer).close();
    }

    @Test
    void stopsWithoutCommittingAndClosesConsumerWhenPollThrows() {
        when(consumerFactory.createConsumer())
                .thenReturn(consumer);
        when(consumer.poll(any(Duration.class)))
                .thenThrow(new IllegalStateException("broker down"));

        assertEquals(0, adapter.pullAndProcess(processor));

        verify(consumer, never()).commitSync();
        verify(consumer).close();
    }

    private ConsumerRecords<String, String> records(String value) {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("events", 0, 0L, "key", value);
        return new ConsumerRecords<>(Map.of(new TopicPartition("events", 0), List.of(record)));
    }

    private ConsumerRecords<String, String> empty() {
        return new ConsumerRecords<>(Collections.emptyMap());
    }
}