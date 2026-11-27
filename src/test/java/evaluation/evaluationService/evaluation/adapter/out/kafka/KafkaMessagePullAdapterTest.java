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
import java.util.ArrayList;
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
    void deserializesValidRecordIgnoringUnknownFieldsAndCountsItAsProcessed() throws Exception {
        ConsumerRecords<String, String> records = records(
                "{\"eventId\":\"event-1\",\"targetTitle\":\"Target\",\"sourceTitle\":\"Source\",\"extraField\":\"ignored\"}");

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
    void resetsEmptyPollCountWhenRecordsArriveAndCommitsEachNonEmptyBatch() throws Exception {
        String raw1 = "{\"eventId\":\"event-1\",\"targetTitle\":\"Target 1\",\"sourceTitle\":\"Source 1\"}";
        String raw2 = "{\"eventId\":\"event-2\",\"targetTitle\":\"Target 2\",\"sourceTitle\":\"Source 2\"}";

        when(consumerFactory.createConsumer())
                .thenReturn(consumer);
        when(consumer.poll(any(Duration.class)))
                .thenReturn(empty(), empty(), records(raw1), empty(), records(raw2), empty(), empty(), empty());

        assertEquals(2, adapter.pullAndProcess(processor));

        verify(processor).process(new RecoveryCompletedEvent("event-1", "Target 1", "Source 1"));
        verify(processor).process(new RecoveryCompletedEvent("event-2", "Target 2", "Source 2"));
        verify(consumer, times(2)).commitSync();
        verify(consumer).close();
    }

    @Test
    void processesMixedBatchCountingOnlySucceededRecordsAndSendingFailedOnesToOnFail() throws Exception {
        String valid = "{\"eventId\":\"event-ok\",\"targetTitle\":\"Target\",\"sourceTitle\":\"Source\"}";
        String malformed = "{invalid-json";
        String processorFailing = "{\"eventId\":\"event-fail\",\"targetTitle\":\"T2\",\"sourceTitle\":\"S2\"}";

        when(consumerFactory.createConsumer())
                .thenReturn(consumer);
        when(consumer.poll(any(Duration.class)))
                .thenReturn(records(valid, malformed, processorFailing), empty(), empty(), empty());
        doNothing()
                .when(processor).process(new RecoveryCompletedEvent("event-ok", "Target", "Source"));
        doThrow(new IllegalStateException("db error"))
                .when(processor).process(new RecoveryCompletedEvent("event-fail", "T2", "S2"));

        assertEquals(1, adapter.pullAndProcess(processor));

        verify(processor).process(new RecoveryCompletedEvent("event-ok", "Target", "Source"));
        verify(processor).onFail(malformed);
        verify(processor).onFail(processorFailing);
        verify(consumer, times(1)).commitSync();
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

    private ConsumerRecords<String, String> records(String... values) {
        List<ConsumerRecord<String, String>> list = new ArrayList<>();
        for (int i = 0; i < values.length; i++) {
            list.add(new ConsumerRecord<>("events", 0, i, "key", values[i]));
        }
        return new ConsumerRecords<>(Map.of(new TopicPartition("events", 0), list));
    }

    private ConsumerRecords<String, String> empty() {
        return new ConsumerRecords<>(Collections.emptyMap());
    }
}
