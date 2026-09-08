package ru.yandex.practicum.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.config.KafkaProperties;

import java.sql.SQLOutput;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Getter
@Component
public class UserActionProcessor implements Runnable {

    KafkaProperties properties;
    private final Consumer<String, ru.practicum.ewm.stats.avro.UserActionAvro> consumer;
    private final String topic = "stats.user-actions.v1";

    // Флаг для корректной остановки потока
    private final AtomicBoolean running = new AtomicBoolean(true);

    public UserActionProcessor(
            @Qualifier("userActionConsumer")
            Consumer<String, ru.practicum.ewm.stats.avro.UserActionAvro> consumer) {
        this.consumer = consumer;
    }
    @Override
    public void run() {
        log.info("UserActionProcessor started, subscribing to topic: {}", topic);
        consumer.subscribe(java.util.Collections.singletonList(topic));

        try {
            while (running.get()) {
                var records = consumer.poll(Duration.ofSeconds(5));
                for (var record : records) {
                    String key = record.key();
                    ru.practicum.ewm.stats.avro.UserActionAvro value = record.value();

                    if (value == null) {
                        log.warn("Received null value for key={} in topic={}", key, topic);
                        continue;
                    }

                    log.info("Processing user action: key={}, offset={}, partition={}",
                            key, record.offset(), record.partition());

                    // TODO: здесь логика обновления БД на основе UserActionAvro
                    processUserAction(key, value);
                }
            }
        } catch (Exception e) {
            log.error("Error in UserActionProcessor loop", e);
        } finally {
            log.info("UserActionProcessor shutting down");
            consumer.close();
        }
    }

    private void processUserAction(String key, ru.practicum.ewm.stats.avro.UserActionAvro action) {
        // Здесь твоя бизнес-логика:
        // - найти пользователя по key
        // - обновить максимальную оценку/вес действия для мероприятия
        // - сохранить в БД
        /*log.trace("Processing action for user={}, event={}, weight={}",
                key, action.getEventId(), action.getWeight());*/
    }

    public void stop() {
        running.set(false);
    }
}
