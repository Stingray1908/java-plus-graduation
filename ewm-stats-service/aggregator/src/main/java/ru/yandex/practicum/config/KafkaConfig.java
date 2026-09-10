package ru.yandex.practicum.config;

import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Configuration
public class KafkaConfig {
    private final KafkaProperties kafkaProperties;

    public KafkaConfig(KafkaProperties kafkaProperties) {
        this.kafkaProperties = kafkaProperties;
    }

    @Bean
    public KafkaConsumer<Long, ru.practicum.ewm.stats.avro.UserActionAvro> kafkaConsumer(KafkaProperties properties) {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getConsumer().getBootstrapServers());
        config.put(ConsumerConfig.GROUP_ID_CONFIG, properties.getConsumer().getGroupId());
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, properties.getConsumer().getAutoOffsetReset());
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, properties.getConsumer().isEnableAutoCommit());

        return new KafkaConsumer<>(
                config,
                new LongDeserializer(),
                new ru.yandex.practicum.config.UserActionAvroDeserializer()
        );
    }


    @Bean
    public KafkaProducer<Long, EventSimilarityAvro> kafkaProducer() {
        Properties props = new Properties();

        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                kafkaProperties.getProducer().getBootstrapServers());

        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                kafkaProperties.getProducer().getKeySerializer());

        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                kafkaProperties.getProducer().getValueSerializer());

        props.put(ProducerConfig.ACKS_CONFIG, String.valueOf(kafkaProperties.getProducer().getAcks()));
        props.put(ProducerConfig.RETRIES_CONFIG, String.valueOf(kafkaProperties.getProducer().getRetries()));
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, String.valueOf(kafkaProperties.getProducer().getBatchSize()));

        return new KafkaProducer<>(props);
    }
}
