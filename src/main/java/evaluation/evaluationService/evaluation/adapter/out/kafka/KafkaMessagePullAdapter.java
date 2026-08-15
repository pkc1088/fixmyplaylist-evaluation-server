package evaluation.evaluationService.evaluation.adapter.out.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import evaluation.evaluationService.evaluation.application.port.out.EventProcessor;
import evaluation.evaluationService.evaluation.application.port.out.MessagePullPort;
import evaluation.evaluationService.evaluation.application.port.out.dto.RecoveryCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.kafka.core.ConsumerFactory;

import java.time.Duration;
import java.util.Collections;

@Slf4j
@RequiredArgsConstructor
public class KafkaMessagePullAdapter implements MessagePullPort {

    private final ConsumerFactory<String, String> consumerFactory;
    private final ObjectMapper objectMapper;
    private final String topicName;

    private static final int MAX_EMPTY_POLLS = 3;

    // 콜백 실행
    @Override
    public int pullAndProcess(EventProcessor processor) {
        int processedCount = 0;

        try (Consumer<String, String> consumer = consumerFactory.createConsumer()) {
            consumer.subscribe(Collections.singletonList(topicName));
            processedCount = pollLoop(consumer, processor);

        } catch (Exception e) {
            log.error("Fatal Error During Manual Consumption", e);
        }

        return processedCount;
    }

    private int pollLoop(Consumer<String, String> consumer, EventProcessor processor) {
        int processedCount = 0;
        int emptyPollCount = 0;

        log.info("[Manual Polling Start...]");

        while (emptyPollCount < MAX_EMPTY_POLLS) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(3));

            if (records.isEmpty()) {
                log.info("Poll returned empty. (Count: {}/{})", ++emptyPollCount, MAX_EMPTY_POLLS);
                continue;
            }

            emptyPollCount = 0;

            for (ConsumerRecord<String, String> record : records) {
                if (processSingleRecord(record, processor)) {
                    processedCount++;
                }
            }

            consumer.commitSync();
        }

        return processedCount;
    }

    private boolean processSingleRecord(ConsumerRecord<String, String> record, EventProcessor processor) {
        try {
            RecoveryCompletedEvent event = objectMapper.readValue(record.value(), RecoveryCompletedEvent.class);
            processor.process(event);
            return true;

        } catch (Exception e) {
            log.error("Event Fail. Offset: {} - Move To DLQ.", record.offset(), e);
            processor.onFail(record.value());
            return false;
        }
    }
}
