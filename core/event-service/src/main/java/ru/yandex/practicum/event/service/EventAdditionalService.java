package ru.yandex.practicum.event.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.StatsClient;
import ru.yandex.practicum.dto.ViewStats;
import ru.yandex.practicum.dto.events.EventFullDto;
import ru.yandex.practicum.dto.events.EventShortDto;
import ru.yandex.practicum.event.entity.Event;
import ru.yandex.practicum.enums.EventState;
import ru.yandex.practicum.error.exception.NotFoundException;
import ru.yandex.practicum.feigns.main.rate.RateAdditionalFeign;
import ru.yandex.practicum.feigns.request.RequestAdditionalFeign;
import ru.yandex.practicum.event.mapper.EventsMapper;
import ru.yandex.practicum.event.repo.EventsRepository;
import ru.yandex.practicum.rating.service.RateService;
import ru.yandex.practicum.rating.service.RateServiceImpl;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static ru.yandex.practicum.event.mapper.EventsMapper.toEventFullDto;
import static ru.yandex.practicum.event.mapper.EventsMapper.toShortEventDto;

@Slf4j

@Service
public class EventAdditionalService {

    private final StatsClient statsClient;
    private final RequestAdditionalFeign requestAdditionalFeign;
    private final EventsRepository eventsRepository;
    private final RateServiceImpl rateService;

    public EventAdditionalService(@Qualifier("StatsClientDiscovery") StatsClient statsClient, RequestAdditionalFeign requestAdditionalFeign, EventsRepository eventsRepository, RateServiceImpl rateService) {
        this.statsClient = statsClient;
        this.requestAdditionalFeign = requestAdditionalFeign;
        this.eventsRepository = eventsRepository;
        this.rateService = rateService;
    }

    public EventFullDto findEventById(Long id) {
        Event event = eventsRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Comment not found"));
        EventFullDto dto = toEventFullDto(event);
        return dto;
    }

    public List<EventFullDto> getEventsByIds(List<Long> ids) {
        List<Event> events = eventsRepository.findAllById(ids);
        return events.stream()
                .map(EventsMapper::toEventFullDto)
                .toList();
    }

    public List<EventShortDto> getEventShortDtoByIdsWithStats(List<Long> ids) {
        List<Long> uniqueIds = ids.stream().distinct().toList();
        List<Event> events = eventsRepository.findAllById(uniqueIds);

        Map<Long, Long> confirmedRequests = getConfirmedRequestsMap(uniqueIds);
        Map<Long, Long> views = getViewsMap(uniqueIds);
        Map<Long, Long> ratings = getRatingsMap(uniqueIds);

        return events.stream()
                .map(event -> toShortEventDto(event,
                        confirmedRequests.getOrDefault(event.getId(), 0L),
                        ratings.getOrDefault(event.getId(), 0L),
                        views.getOrDefault(event.getId(), 0L)))
                .toList();
    }

    /*public List<EventFullDto> findEventsBySubscriberIdAndStatus(Long ids,
                                                                EventState state,
                                                                LocalDateTime time,
                                                                PageRequest request) {
        List<Event> events = eventsRepository.findEventsBySubscriberIdAndStatusAndTimeAfter(ids, state, time, request);
        return events.stream()
                .map(EventsMapper::toEventFullDto)
                .toList();
    }*/


    private Map<Long, Long> getConfirmedRequestsMap(List<Long> events) {
        if (events.isEmpty()) return Map.of();

        List<Object[]> results = requestAdditionalFeign.countRequestsByEventIdsAndStatus(events, EventState.CONFIRMED);

        return results.stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> ((Number) row[1]).longValue()
                ));
    }

    private Map<Long, Long> getViewsMap(List<Long> events) {
        if (events.isEmpty()) return Map.of();

        List<String> uris = events.stream()
                .map(e -> "/events/" + e)
                .collect(toList());

        LocalDateTime start = LocalDateTime.now().minusYears(10);
        LocalDateTime end = LocalDateTime.now();

        List<ViewStats> stats;
        try {
            stats = statsClient.getStats(start, end, uris, true);
        } catch (Exception e) {
            log.error("Ошибка при получении статистики", e);
            return Map.of();
        }

        Map<Long, Long> viewsMap = new HashMap<>();
        for (ViewStats stat : stats) {
            String uri = stat.getUri();
            if (uri.startsWith("/events/")) {
                try {
                    Long eventId = Long.parseLong(uri.substring("/events/".length()));
                    viewsMap.put(eventId, stat.getHits());
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return viewsMap;
    }

    private Map<Long, Long> getRatingsMap(List<Long> eventIds) {
        if (eventIds.isEmpty()) return Map.of();

        List<Object[]> results = rateService.getRatingsForEvents(eventIds);
        return results.stream().collect(Collectors.toMap(
                row -> ((Number) row[0]).longValue(),
                row -> ((Number) row[1]).longValue()
        ));
    }

}
