package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.entity.EventSimilarity;
import ru.yandex.practicum.entity.UserAction;
import ru.yandex.practicum.repo.EventSimilarityRepository;
import ru.yandex.practicum.repo.UserActionRepository;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationsService {

    private final EventSimilarityRepository similarityRepository;
    private final UserActionRepository userActionRepository;

    private static final Map<String, Double> WEIGHTS = Map.of(
            "VIEW", 0.1,
            "REGISTER", 0.2,
            "LIKE", 0.5
    );

    /**
     * GetRecommendationsForUser:
     * 1. Берём историю пользователя (максимальный вес на каждое событие).
     * 2. Для каждого события из истории находим похожие.
     * 3. Взвешиваем score сходства на вес действия пользователя.
     * 4. Суммируем по кандидатам, исключаем уже взаимодействованные.
     * 5. Сортируем по убыванию, берём top N.
     */
    public List<ScoredEvent> getRecommendationsForUser(long userId, int maxResults) {
        List<UserAction> actions = userActionRepository.findByUserId(userId);
        if (actions.isEmpty()) {
            return Collections.emptyList();
        }

        // eventId -> максимальный вес действия пользователя
        Map<Long, Double> userEventWeights = new HashMap<>();
        for (UserAction action : actions) {
            double weight = WEIGHTS.getOrDefault(action.getActionType(), 0.0);
            userEventWeights.merge(action.getEventId(), weight, Math::max);
        }

        // кандидат -> суммарный взвешенный score
        Map<Long, Double> candidates = new HashMap<>();

        for (Map.Entry<Long, Double> entry : userEventWeights.entrySet()) {
            long userEventId = entry.getKey();
            double userWeight = entry.getValue();

            List<EventSimilarity> sims = similarityRepository.findByEventId(userEventId);
            for (EventSimilarity sim : sims) {
                long otherEvent = sim.getEventA() == userEventId
                        ? sim.getEventB()
                        : sim.getEventA();

                if (userEventWeights.containsKey(otherEvent)) continue;

                double weightedScore = sim.getScore() * userWeight;
                candidates.merge(otherEvent, weightedScore, Double::sum);
            }
        }

        return candidates.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(maxResults)
                .map(e -> new ScoredEvent(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * GetSimilarEvents:
     * Находим события, похожие на eventId, исключая те,
     * с которыми пользователь уже взаимодействовал.
     * Возвращаем с коэффициентом сходства.
     */
    public List<ScoredEvent> getSimilarEvents(long eventId, long userId, int maxResults) {
        List<EventSimilarity> sims = similarityRepository.findByEventIdOrderByScoreDesc(eventId);
        if (sims.isEmpty()) {
            return Collections.emptyList();
        }

        // События, с которыми пользователь уже взаимодействовал
        Set<Long> interactedEvents = userActionRepository.findByUserId(userId).stream()
                .map(UserAction::getEventId)
                .collect(Collectors.toSet());

        return sims.stream()
                .map(sim -> {
                    long otherEvent = sim.getEventA() == eventId
                            ? sim.getEventB()
                            : sim.getEventA();
                    return new ScoredEvent(otherEvent, sim.getScore());
                })
                .filter(se -> !interactedEvents.contains(se.eventId()))
                .limit(maxResults)
                .collect(Collectors.toList());
    }

    /**
     * GetInteractionsCount:
     * Для каждого запрошенного мероприятия — сумма максимальных
     * весов действий всех пользователей с ним.
     */
    public List<ScoredEvent> getInteractionsCount(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<UserAction> actions = userActionRepository.findByEventIdIn(eventIds);

        // eventId -> (userId -> максимальный вес)
        Map<Long, Map<Long, Double>> eventUserMaxWeight = new HashMap<>();
        for (UserAction action : actions) {
            double weight = WEIGHTS.getOrDefault(action.getActionType(), 0.0);
            eventUserMaxWeight
                    .computeIfAbsent(action.getEventId(), k -> new HashMap<>())
                    .merge(action.getUserId(), weight, Math::max);
        }

        // eventId -> сумма максимальных весов
        Map<Long, Double> result = new HashMap<>();
        for (long eventId : eventIds) {
            Map<Long, Double> userWeights = eventUserMaxWeight.get(eventId);
            double sum = userWeights == null ? 0.0 : userWeights.values().stream().mapToDouble(Double::doubleValue).sum();
            result.put(eventId, sum);
        }

        return eventIds.stream()
                .map(eid -> new ScoredEvent(eid, result.getOrDefault(eid, 0.0)))
                .collect(Collectors.toList());
    }

    public record ScoredEvent(long eventId, double score) {}
}
