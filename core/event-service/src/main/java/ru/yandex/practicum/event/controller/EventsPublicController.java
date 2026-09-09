package ru.yandex.practicum.event.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.CollectorClient;
import ru.yandex.practicum.StatsClient;
import ru.yandex.practicum.dto.EndpointHit;
import ru.yandex.practicum.dto.events.EventFullDto;
import ru.yandex.practicum.dto.events.EventShortDto;
import ru.yandex.practicum.enums.EventsSortType;
import ru.yandex.practicum.event.service.EventsService;
import ru.yandex.practicum.feigns.event.EventsPublicFeign;
import stats.service.collector.UserActionProtoOuterClass;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.random.RandomGenerator;

@RestController
@RequestMapping("/events")
public class EventsPublicController implements EventsPublicFeign {

    private final EventsService eventService;
    private final StatsClient statsClient;
    private final CollectorClient collectorClient;

    @Value("${spring.application.name}")
    private String appName;

    public EventsPublicController(
            EventsService eventService,
            @Qualifier("StatsClientDiscovery") StatsClient statsClient, CollectorClient collectorClient
    ) {
        this.eventService = eventService;
        this.statsClient = statsClient;
        this.collectorClient = collectorClient;
    }

    @PutMapping("/{eventId}/like")
    public ResponseEntity<Void> likeEvent(
            @RequestHeader("X-EWM-USER-ID") long userId,
            @PathVariable Long eventId
    ) {
        eventService.likeEvent(userId, eventId);
        sendUserAction(userId, eventId, UserActionProtoOuterClass.ActionTypeProto.ACTION_LIKE);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<EventShortDto>> getEvents(
            @RequestParam(required = false)
            @Size(max = 1000, message = "Text length must be less than or equal to 1000 characters")
            String text,

            @RequestParam(required = false)
            List<@Positive(message = "Category IDs must be positive numbers") Long> categories,

            @RequestParam(required = false) Boolean paid,

            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            LocalDateTime rangeStart,

            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            LocalDateTime rangeEnd,

            @RequestParam(defaultValue = "false") Boolean onlyAvailable,

            @RequestParam(defaultValue = "EVENT_DATE")
            @Pattern(regexp = "EVENT_DATE|VIEWS|RATING", message = "Sort must be either 'EVENT_DATE', 'VIEWS' or 'RATING'")
            String sort,

            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "From must be greater than or equal to 0")
            Integer from,

            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "Size must be greater than 0")
            @Max(value = 1000, message = "Size must be less than or equal to 1000")
            Integer size,

            HttpServletRequest request
    ) {
        /*EndpointHit hit = EndpointHit.builder()
                .app("ewm-main-service")
                .uri(request.getRequestURI())
                .ip(request.getRemoteAddr())
                .timestamp(LocalDateTime.now())
                .build();
        statsClient.hit(hit);*/

       /* Random random = new Random();
        long i = random.nextLong(1000);
        sendUserAction(i, i, UserActionProtoOuterClass.ActionTypeProto.ACTION_VIEW);
*/
        List<EventShortDto> events = eventService.getPublishedEvents(
                text, categories, paid, rangeStart, rangeEnd, onlyAvailable,
                EventsSortType.valueOf(sort), from, size
        );

        return ResponseEntity.ok(events);
    }

    @Override
    public ResponseEntity<EventFullDto> getEventByIdInside(
            @PathVariable Long id) {

        EventFullDto event = eventService.findEventById(id);
        return ResponseEntity.ok(event);
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<EventFullDto> getEventById(
            @RequestHeader("X-EWM-USER-ID") long userId,
            @PathVariable Long eventId,
            HttpServletRequest request
    ) {
        /*EndpointHit hit = EndpointHit.builder()
                .app("ewm-main-service")
                .uri(request.getRequestURI())
                .ip(request.getRemoteAddr())
                .timestamp(LocalDateTime.now())
                .build();
        statsClient.hit(hit);*/

        sendUserAction(userId, eventId, UserActionProtoOuterClass.ActionTypeProto.ACTION_VIEW);
        EventFullDto event = eventService.getPublishedEventById(eventId);
        return ResponseEntity.ok(event);
    }

    private void sendUserAction (long userId, long eventId, UserActionProtoOuterClass.ActionTypeProto actionType){
        collectorClient.sendUserAction(
                userId,
                eventId,
                actionType,
                Instant.now());
    }

}
