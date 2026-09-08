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

    @Getter @Setter
    public static class Topics {
        private String userActions;
        private String eventsSimilarity;
    }

    @Getter @Setter
    public static class Consumer {
        private String bootstrapServers;
        private String groupId;
        private String autoOffsetReset = "earliest";
        private boolean enableAutoCommit = false;
    }

    @Getter
    @Setter
    public static class Producer {
        private String bootstrapServers;
        private int acks;
        private int retries;
        private int batchSize;
        private String keySerializer;
        private String valueSerializer;
    }
}
