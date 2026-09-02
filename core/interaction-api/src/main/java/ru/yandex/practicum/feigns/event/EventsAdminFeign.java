package ru.yandex.practicum.feigns.event;

import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.dto.events.EventFullDto;
import ru.yandex.practicum.dto.events.EventShortDto;
import ru.yandex.practicum.dto.events.UpdateEventAdminRequest;

import java.time.LocalDateTime;
import java.util.List;

@FeignClient(name = "event-service", contextId = "eventsAdminFeign", path = "/admin/events")
public interface EventsAdminFeign {

    @GetMapping("/{eventId}")
    ResponseEntity<EventFullDto> getEventById(@PathVariable Long eventId);

    @GetMapping
    public ResponseEntity<List<EventFullDto>> getEvents(
            @RequestParam(required = false) List<Long> users,
            @RequestParam(required = false) List<String> states,
            @RequestParam(required = false) List<Long> categories,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeStart,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeEnd,
            @RequestParam(defaultValue = "0") Integer from,
            @RequestParam(defaultValue = "10") Integer size
    );

    @PatchMapping("/{eventId}")
    public ResponseEntity<EventFullDto> updateEventByAdmin(
            @PathVariable Long eventId,
            @Valid @RequestBody UpdateEventAdminRequest updateRequest
    );

    @GetMapping("/moderation")
    @ResponseStatus(HttpStatus.OK)
    public List<EventFullDto> getEventsForModeration(
            @RequestParam(defaultValue = "0") Integer from,
            @RequestParam(defaultValue = "10") Integer size
    );

    @GetMapping("/all-in")
    @ResponseStatus(HttpStatus.OK)
    public List<EventFullDto> getEventsByIds(
            @RequestParam(required = true) List<Long> ids);
    //пишем гет по ид с простым дто, чтобы проверить вообще существует или нет,
    // и не задействовать стат сервис

    @GetMapping("/stats")
    @ResponseStatus(HttpStatus.OK)
    public List<EventShortDto> getEventShortDtoByIdsWithStats(@RequestParam(required = true) List<Long> ids);

}
