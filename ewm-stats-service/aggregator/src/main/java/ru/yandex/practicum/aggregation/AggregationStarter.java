package ru.yandex.practicum.aggregation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.WakeupException;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.yandex.practicum.config.KafkaProperties;
import org.springframework.stereotype.Component;
import jakarta.annotation.PreDestroy;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AggregationStarter {

    private final KafkaConsumer<Long, UserActionAvro> consumer;
    private final KafkaProducer<Long, EventSimilarityAvro> producer;
    private final KafkaProperties properties;
    private final SimilarityCalculator calculator = new SimilarityCalculator();

    private volatile boolean running = true;

    public void start() {
        String topic = properties.getTopics().getUserActions();
        String outputTopic = properties.getTopics().getEventsSimilarity();

        consumer.subscribe(Collections.singletonList(topic));
        log.info("Aggregator подписался на топик: {}", topic);

        try {
            while (running) {
                ConsumerRecords<Long, UserActionAvro> records =
                        consumer.poll(Duration.ofMillis(2000));

                if (records.isEmpty()) continue;

                for (ConsumerRecord<Long, UserActionAvro> record : records) {
                    UserActionAvro action = record.value();
                    log.info("Получено действие: userId={}, eventId={}, action={}",
                            action.getUserId(), action.getEventId(), action.getActionType());

                    List<EventSimilarityAvro> updates = calculator.processAction(action);

                    for (EventSimilarityAvro sim : updates) {
                        ProducerRecord<Long, EventSimilarityAvro> producerRecord =
                                new ProducerRecord<>(
                                        outputTopic,
                                        null,
                                        sim.getTimestamp().toEpochMilli(),
                                        null,
                                        sim);
                        producer.send(producerRecord, (metadata, e) -> {
                            if (e != null) {
                                log.info("Ошибка отправки сходства в топик {}", outputTopic, e);
                            } else {
                                log.info("Отправлено сходство: A={}, B={}, score={}",
                                        sim.getEventA(), sim.getEventB(), sim.getScore());
                            }
                        });
                    }
                }

                producer.flush();
                consumer.commitSync();
                log.debug("Offsets закоммичены, обработано записей: {}", records.count());
            }
        } catch (WakeupException e) {
            log.info("Получен WakeupException, завершаем работу");
        } catch (Exception e) {
            log.error("Ошибка в цикле агрегатора", e);
        } finally {
            close();
        }
    }

    @PreDestroy
    public void close() {
        running = false;
        consumer.wakeup();
        producer.close();
        consumer.close();
        log.info("Aggregator остановлен");
    }
}
