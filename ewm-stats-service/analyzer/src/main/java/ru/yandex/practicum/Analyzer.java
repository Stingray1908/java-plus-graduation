package ru.yandex.practicum;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.ConfigurableApplicationContext;
import ru.yandex.practicum.processor.EventSimilarityProcessor;
import ru.yandex.practicum.processor.UserActionProcessor;

@ConfigurationPropertiesScan
    @Slf4j
    @SpringBootApplication
    public class Analyzer {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(Analyzer.class, args);

            UserActionProcessor userActionProcessor = context.getBean(UserActionProcessor.class);
            EventSimilarityProcessor eventSimilarityProcessor = context.getBean(EventSimilarityProcessor.class);

            Consumer<?, ?> userActionConsumer = userActionProcessor.getConsumer();
            Consumer<?, ?> eventSimilarityConsumer = eventSimilarityProcessor.getConsumer();

            log.info("Consumers initialized: userActionConsumer={}, eventSimilarityConsumer={}", userActionConsumer != null, eventSimilarityConsumer != null);
            if (userActionConsumer != null) {
                log.info("SnapshotConsumer subscribed topics: {}", userActionConsumer.subscription());
            }
            if (eventSimilarityConsumer != null) {
                log.info("HubEventConsumer subscribed topics: {}", eventSimilarityConsumer.subscription());
            }

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                log.info("Shutdown hook triggered. Waking up consumers...");
                if (userActionConsumer != null) userActionConsumer.wakeup();
                if (eventSimilarityConsumer != null) eventSimilarityConsumer.wakeup();
            }));

            Thread hubEventsThread = new Thread(userActionProcessor);
            hubEventsThread.setName("UserActionHandlerThread");
            hubEventsThread.start();

            Thread snapshotThread = new Thread(eventSimilarityProcessor);
            snapshotThread.setName("EventSimilarityThread");
            snapshotThread.start();
        }
    }
