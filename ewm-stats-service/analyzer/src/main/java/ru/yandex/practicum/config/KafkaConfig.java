package ru.yandex.practicum.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    private final KafkaProperties kafkaProperties;

    public KafkaConfig(KafkaProperties kafkaProperties) {
        this.kafkaProperties = kafkaProperties;
    }

    private Map<String, Object> baseConsumerConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                kafkaProperties.getConsumer().getBootstrapServers());
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                kafkaProperties.getConsumer().getAutoOffsetReset());
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,
                kafkaProperties.getConsumer().isEnableAutoCommit());
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                LongDeserializer.class);
        return config;
    }

    @Bean
    public KafkaConsumer<Long, EventSimilarityAvro> eventSimilarityConsumer() {
        Map<String, Object> config = baseConsumerConfig();
        config.put(ConsumerConfig.GROUP_ID_CONFIG,
                kafkaProperties.getConsumer().getSimilarityGroupId());
        return new KafkaConsumer<>(
                config,
                new LongDeserializer(),
                new EventSimilarityAvroDeserializer()
        );
    }

    @Bean
    public KafkaConsumer<Long, UserActionAvro> userActionConsumer() {
        Map<String, Object> config = baseConsumerConfig();
        config.put(ConsumerConfig.GROUP_ID_CONFIG,
                kafkaProperties.getConsumer().getActionsGroupId());
        return new KafkaConsumer<>(
                config,
                new LongDeserializer(),
                new UserActionAvroDeserializer()
        );
    }
}
