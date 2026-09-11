package ru.yandex.practicum.aggregation;

import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Instant;
import java.util.*;

public class SimilarityCalculator {

    private static final Map<ActionTypeAvro, Double> WEIGHTS = Map.of(
            ActionTypeAvro.VIEW,     0.4,
            ActionTypeAvro.REGISTER, 0.8,
            ActionTypeAvro.LIKE,     1.0
    );

    // eventId -> (userId -> максимальный вес действия пользователя)
    private final Map<Long, Map<Long, Double>> eventUserWeights = new HashMap<>();

    // eventId -> сумма весов всех действий с этим мероприятием (S_A)
    private final Map<Long, Double> eventSums = new HashMap<>();

    // minEventId -> (maxEventId -> S_min для пары)
    private final Map<Long, Map<Long, Double>> minWeightsSums = new HashMap<>();

    public List<EventSimilarityAvro> processAction(UserActionAvro action) {
        List<EventSimilarityAvro> updates = new ArrayList<>();

        long userId       = action.getUserId();
        long eventId      = action.getEventId();
        double actionWeight = WEIGHTS.get(action.getActionType());

        Map<Long, Double> userWeights =
                eventUserWeights.computeIfAbsent(eventId, k -> new HashMap<>());

        double prevWeight = userWeights.getOrDefault(userId, 0.0);

        if (actionWeight <= prevWeight) {
            return updates;
        }

        // --- 1. Обновляем S_A (сумму весов мероприятия) ---
        double oldS_A = eventSums.getOrDefault(eventId, 0.0);
        double newS_A = oldS_A - prevWeight + actionWeight;
        eventSums.put(eventId, newS_A);

        // --- 2. Для каждого мероприятия B, с которым пользователь
        //        также взаимодействовал, обновляем S_min(A, B) ---
        for (Map.Entry<Long, Map<Long, Double>> entry : eventUserWeights.entrySet()) {
            long otherEventId = entry.getKey();
            if (otherEventId == eventId) continue;

            Double otherUserWeight = entry.getValue().get(userId);
            if (otherUserWeight == null) continue; // пользователь не взаимодействовал с B

            double oldMin = Math.min(prevWeight, otherUserWeight);
            double newMin = Math.min(actionWeight, otherUserWeight);
            double delta  = newMin - oldMin;

            if (delta != 0.0) {
                double currentSMin = getMinWeight(eventId, otherEventId);
                putMinWeight(eventId, otherEventId, currentSMin + delta);
            }
        }

        // --- 3. Обновляем максимальный вес пользователя для мероприятия ---
        userWeights.put(userId, actionWeight);

        // --- 4. Пересчитываем сходство со всеми остальными мероприятиями ---
        for (long otherEventId : eventUserWeights.keySet()) {
            if (otherEventId == eventId) continue;

            // Только если пользователь взаимодействовал с обоими мероприятиями
            Map<Long, Double> otherUsers = eventUserWeights.get(otherEventId);
            if (otherUsers == null || !otherUsers.containsKey(userId)) continue;

            double sMin = getMinWeight(eventId, otherEventId);
            double sB   = eventSums.getOrDefault(otherEventId, 0.0);

            double score;
            if (newS_A <= 0.0 || sB <= 0.0) {
                score = 0.0;
            } else {
                score = sMin / (Math.sqrt(newS_A) * Math.sqrt(sB));
            }

            if (score > 0.0) {
                updates.add(EventSimilarityAvro.newBuilder()
                        .setEventA(Math.min(eventId, otherEventId))
                        .setEventB(Math.max(eventId, otherEventId))
                        .setScore(score)
                        .setTimestamp(Instant.now())
                        .build());
            }
        }

        return updates;
    }

    private void putMinWeight(long eventA, long eventB, double sum) {
        long first  = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);
        minWeightsSums
                .computeIfAbsent(first, k -> new HashMap<>())
                .put(second, sum);
    }

    private double getMinWeight(long eventA, long eventB) {
        long first  = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);
        return minWeightsSums
                .computeIfAbsent(first, k -> new HashMap<>())
                .getOrDefault(second, 0.0);
    }
}
