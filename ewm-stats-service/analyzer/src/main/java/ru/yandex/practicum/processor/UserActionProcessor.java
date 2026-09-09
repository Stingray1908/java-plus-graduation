package ru.yandex.practicum.processor;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.yandex.practicum.entity.UserAction;
import ru.yandex.practicum.repo.UserActionRepository;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Getter
@Component
public class UserActionProcessor implements Runnable {

    private final Consumer<String, UserActionAvro> consumer;
    private final UserActionRepository userActionRepository;
    private final String topic = "stats.user-actions.v1";
    private final AtomicBoolean running = new AtomicBoolean(true);

    private static final Map<ActionTypeAvro, Double> WEIGHTS = Map.of(
            ActionTypeAvro.VIEW, 0.1,
            ActionTypeAvro.REGISTER, 0.2,
            ActionTypeAvro.LIKE, 0.5
    );

    public UserActionProcessor(
            @Qualifier("userActionConsumer")
            Consumer<String, UserActionAvro> consumer,
            UserActionRepository userActionRepository) {
        this.consumer = consumer;
        this.userActionRepository = userActionRepository;
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
                    UserActionAvro value = record.value();

                    if (value == null) {
                        log.warn("Received null value for key={} in topic={}", key, topic);
                        continue;
                    }

                    log.info("Processing user action: key={}, offset={}, partition={}",
                            key, record.offset(), record.partition());

                    processUserAction(key, value);
                }
                consumer.commitSync();
            }
        } catch (Exception e) {
            log.error("Error in UserActionProcessor loop", e);
        } finally {
            log.info("UserActionProcessor shutting down");
            consumer.close();
        }
    }

    private void processUserAction(String key, UserActionAvro action) {
        long userId = action.getUserId();
        long eventId = action.getEventId();
        String actionType = action.getActionType().name();
        double newWeight = WEIGHTS.get(action.getActionType());

        Optional<UserAction> existing = userActionRepository.findByUserIdAndEventId(userId, eventId);

        if (existing.isPresent()) {
            double oldWeight = WEIGHTS.get(ActionTypeAvro.valueOf(existing.get().getActionType()));
            if (newWeight > oldWeight) {
                existing.get().setActionType(actionType);
                existing.get().setTimestamp(action.getTimestamp());
                userActionRepository.save(existing.get());
                log.debug("Updated action: userId={}, eventId={}, newAction={}", userId, eventId, actionType);
            }
        } else {
            UserAction newAction = new UserAction();
            newAction.setUserId(userId);
            newAction.setEventId(eventId);
            newAction.setActionType(actionType);
            newAction.setTimestamp(action.getTimestamp());
            userActionRepository.save(newAction);
            log.debug("Saved action: userId={}, eventId={}, action={}", userId, eventId, actionType);
        }
    }

    public void stop() {
        running.set(false);
    }
}
