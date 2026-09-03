package ru.yandex.practicum.feigns.event;

import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.dto.events.EventFullDto;
import ru.yandex.practicum.dto.events.NewEventDto;
import ru.yandex.practicum.dto.events.UpdateEventUserRequest;
import ru.yandex.practicum.enums.EventState;

import java.time.LocalDateTime;
import java.util.List;

@FeignClient(name = "event-service", contextId = "eventPrivateFeign", path = "/users/{userId}/events")
public interface EventPrivateFeign {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    EventFullDto addEvent(
            @PathVariable("userId") Long userId,
            @Valid @RequestBody NewEventDto newEventDto
    );

    @PatchMapping("/{eventId}")
    @ResponseStatus(HttpStatus.OK)
    EventFullDto updateEvent(
            @PathVariable("userId") Long userId,
            @PathVariable("eventId") Long eventId,
            @Valid @RequestBody UpdateEventUserRequest updateEventUserRequest);

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    List<EventFullDto> getUserEvents(
            @PathVariable("userId") Long userId,
            @RequestParam(defaultValue = "0") Integer from,
            @RequestParam(defaultValue = "10") Integer size);

    @GetMapping("/moderation")
    @ResponseStatus(HttpStatus.OK)
    List<EventFullDto> getUserModerationHistory(
            @PathVariable("userId") Long userId,
            @RequestParam(defaultValue = "0") Integer from,
            @RequestParam(defaultValue = "10") Integer size
    );

    @GetMapping("/{eventId}")
    @ResponseStatus(HttpStatus.OK)
    EventFullDto getUserEventById(
            @PathVariable("userId") Long userId,
            @PathVariable("eventId") Long eventId);

    @GetMapping("/by-id-status-time")
    @ResponseStatus(HttpStatus.OK)
    List<EventFullDto> findEventsBySubscriberIdAndStatusAndTimeAfter(
            @PathVariable("userId") Long userId,
            @RequestParam EventState state,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime now,
            @RequestParam(defaultValue = "0") int from,
            @RequestParam(defaultValue = "10") int size
    );
}
