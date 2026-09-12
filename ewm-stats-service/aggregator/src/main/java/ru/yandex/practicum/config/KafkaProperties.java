package ru.yandex.practicum.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "kafka")
public class KafkaProperties {
    private Topics topics = new Topics();
    private Consumer consumer = new Consumer();
    private Producer producer = new Producer();
    private String bootstrapServer;

    @Getter @Setter
    public static class Topics {
        private String userActions;
        private String eventsSimilarity;
    }

    @Getter @Setter
    public static class Consumer {
        private String groupId;
        private String autoOffsetReset = "earliest";
        private boolean enableAutoCommit = false;
        private String keyDeserializer;
        private String valueDeserializer;
    }

    @Getter
    @Setter
    public static class Producer {
        private int acks;
        private int retries;
        private int batchSize;
        private String keySerializer;
        private String valueSerializer;
    }
}
