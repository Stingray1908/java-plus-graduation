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

    @Getter @Setter
    public static class Topics {
        private String userActions;
        private String eventsSimilarity;
    }

    @Getter @Setter
    public static class Consumer {
        private String bootstrapServers;
        private String autoOffsetReset = "earliest";
        private boolean enableAutoCommit = false;
        private String similarityGroupId;   // ← новое
        private String actionsGroupId;
        private String keyDeserializer;
        private String eventSimilarityDeserializer;
        private String userActionDeserializer;
    }
}
