package ru.yandex.practicum.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "kafka")
public class KafkaProperties {
    private Topics topics = new Topics();
    private Producer producer = new Producer();

    @Getter @Setter
    public static class Topics {
        private String userActions;
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
