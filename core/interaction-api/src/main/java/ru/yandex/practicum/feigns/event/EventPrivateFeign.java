package ru.yandex.practicum.feigns.event;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.dto.events.EventFullDto;
import ru.yandex.practicum.dto.events.NewEventDto;
import ru.yandex.practicum.dto.events.UpdateEventUserRequest;
import ru.yandex.practicum.enums.EventState;

import java.time.LocalDateTime;
import java.util.List;

@FeignClient(name = "event-service", contextId = "eventPrivateFeign", path = "/users/{userId}/events")
public interface EventPrivateFeign {

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    EventFullDto addEvent(
            @PathVariable @Positive Long userId,
            @Valid @RequestBody NewEventDto newEventDto);

    @PatchMapping(value = "/{eventId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    EventFullDto updateEvent(
            @PathVariable @Positive Long userId,
            @PathVariable @Positive Long eventId,
            @Valid @RequestBody UpdateEventUserRequest updateEventUserRequest);

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    List<EventFullDto> getUserEvents(
            @PathVariable @Positive Long userId,
            @RequestParam(defaultValue = "0") @Min(0) Integer from,
            @RequestParam(defaultValue = "10") @Positive Integer size);

    @GetMapping("/moderation")
    @ResponseStatus(HttpStatus.OK)
    List<EventFullDto> getUserModerationHistory(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") Integer from,
            @RequestParam(defaultValue = "10") Integer size
    );

    @GetMapping("/{eventId}")
    @ResponseStatus(HttpStatus.OK)
    EventFullDto getUserEventById(
            @PathVariable @Positive Long userId,
            @PathVariable @Positive Long eventId);

    @GetMapping("/by-id-status-time")
    @ResponseStatus(HttpStatus.OK)
    List<EventFullDto> findEventsBySubscriberIdAndStatusAndTimeAfter(
            @PathVariable @Positive Long userId,
            @RequestParam EventState state,
            @RequestParam LocalDateTime now,
            @RequestParam(defaultValue = "0") @Min(0) int from,
            @RequestParam(defaultValue = "10") @Positive int size
    );
}
