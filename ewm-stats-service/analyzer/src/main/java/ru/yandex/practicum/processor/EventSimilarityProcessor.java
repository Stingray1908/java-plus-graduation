package ru.yandex.practicum.processor;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.yandex.practicum.repo.EventSimilarityRepository;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Getter
@Component
public class EventSimilarityProcessor implements Runnable {

    private final Consumer<String, EventSimilarityAvro> consumer;
    private final EventSimilarityRepository similarityRepository;
    private final String topic = "stats.events-similarity.v1";
    private final AtomicBoolean running = new AtomicBoolean(true);

    public EventSimilarityProcessor(
            @Qualifier("eventSimilarityConsumer")
            Consumer<String, EventSimilarityAvro> consumer,
            EventSimilarityRepository similarityRepository) {
        this.consumer = consumer;
        this.similarityRepository = similarityRepository;
    }

    @Override
    public void run() {
        log.info("EventSimilarityProcessor started, subscribing to topic: {}", topic);
        consumer.subscribe(java.util.Collections.singletonList(topic));

        try {
            while (running.get()) {
                var records = consumer.poll(Duration.ofSeconds(5));
                for (var record : records) {
                    String key = record.key();
                    EventSimilarityAvro value = record.value();

                    if (value == null) {
                        log.warn("Received null value for key={} in topic={}", key, topic);
                        continue;
                    }

                    log.info("Processing similarity: key={}, similarity={}", key, value);
                    processSimilarity(key, value);
                }
                consumer.commitSync();
            }
        } catch (Exception e) {
            log.error("Error in EventSimilarityProcessor loop", e);
        } finally {
            log.info("EventSimilarityProcessor shutting down");
            consumer.close();
        }
    }

    private void processSimilarity(String key, EventSimilarityAvro similarity) {
        long eventA = similarity.getEventA();
        long eventB = similarity.getEventB();
        double score = similarity.getScore();

        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);

        similarityRepository.upsertScore(first, second, score);
        log.debug("Saved similarity: ({}, {}) -> {}", first, second, score);
    }

    public void stop() {
        running.set(false);
    }
}
