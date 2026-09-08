package ru.yandex.practicum.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Getter
@Component
public class EventSimilarityProcessor implements Runnable {

    private final Consumer<String, ru.practicum.ewm.stats.avro.EventSimilarityAvro> consumer;
    private final String topic = "stats.events-similarity.v1";

    private final AtomicBoolean running = new AtomicBoolean(true);

    public EventSimilarityProcessor(
            @Qualifier("eventSimilarityConsumer")
            Consumer<String, ru.practicum.ewm.stats.avro.EventSimilarityAvro> consumer) {
        this.consumer = consumer;
    }

    @Override
    public void run() {
        log.info("EventSimilarityProcessor started, subscribing to topic: {}", topic);
        consumer.subscribe(java.util.Collections.singletonList(topic));

        try {
            while (running.get()) {
                var records = consumer.poll(Duration.ofSeconds(5));
                for (var record : records) {
                    String key = record.key(); // обычно это пара "eventA_eventB"
                    ru.practicum.ewm.stats.avro.EventSimilarityAvro value = record.value();

                    if (value == null) {
                        log.warn("Received null value for key={} in topic={}", key, topic);
                        continue;
                    }

                    log.info("Processing similarity: key={}, similarity={}",
                            key, value);

                    // TODO: сохранить сходство мероприятий в БД
                    processSimilarity(key, value);
                }
            }
        } catch (Exception e) {
            log.error("Error in EventSimilarityProcessor loop", e);
        } finally {
            log.info("EventSimilarityProcessor shutting down");
            consumer.close();
        }
    }

    private void processSimilarity(String key, ru.practicum.ewm.stats.avro.EventSimilarityAvro similarity) {
        // Здесь логика сохранения сходства мероприятий:
        // - распарсить key на eventId1 и eventId2
        // - положить similarity.getSimilarity() в таблицу сходств
        log.trace("Storing similarity for pair={}, value={}", key, similarity);
    }

    public void stop() {
        running.set(false);
    }
}
