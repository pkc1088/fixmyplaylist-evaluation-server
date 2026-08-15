package evaluation.evaluationService.evaluation.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import evaluation.evaluationService.evaluation.adapter.out.kafka.KafkaDlqAdapter;
import evaluation.evaluationService.evaluation.adapter.out.kafka.KafkaMessagePullAdapter;
import evaluation.evaluationService.evaluation.application.port.out.DlqPort;
import evaluation.evaluationService.evaluation.application.port.out.MessagePullPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;

@Slf4j
@Configuration
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
public class ConfluentKafkaConfig {

    @Bean
    public MessagePullPort recoveryCompletedTopic(
            @Value("${app.kafka.topic.recovery-completed}") String topicName,
            ConsumerFactory<String, String> consumerFactory,
            ObjectMapper objectMapper
    ) {
        return new KafkaMessagePullAdapter(consumerFactory, objectMapper, topicName);
    }

    @Bean
    public DlqPort recoveryDlqTopic(
            @Value("${app.kafka.topic.recovery-dlq}") String topicName,
            KafkaTemplate<String, String> kafkaTemplate)
    {
        return new KafkaDlqAdapter(kafkaTemplate, topicName);
    }
}
