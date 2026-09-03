package ru.practicum.events.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.events.dto.EventFullDto;
import ru.practicum.events.dto.UpdateEventAdminRequest;
import ru.practicum.events.service.EventsService;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/admin/events")
@RequiredArgsConstructor
public class AdminEventsController {

    private final EventsService adminEventService;

    @GetMapping
    public ResponseEntity<List<EventFullDto>> getEvents(
            @RequestParam(required = false) List<Long> users,
            @RequestParam(required = false) List<String> states,
            @RequestParam(required = false) List<Long> categories,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeStart,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeEnd,
            @RequestParam(defaultValue = "0") Integer from,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        List<EventFullDto> events = adminEventService.getEvents(users, states, categories, rangeStart, rangeEnd, from, size);
        return ResponseEntity.ok(events);
    }

    @PatchMapping("/{eventId}")
    public ResponseEntity<EventFullDto> updateEventByAdmin(
            @PathVariable Long eventId,
            @Valid @RequestBody UpdateEventAdminRequest updateRequest
    ) {
        EventFullDto updatedEvent = adminEventService.updateEventByAdmin(eventId, updateRequest);
        return ResponseEntity.ok(updatedEvent);
    }

    @GetMapping("/moderation")
    @ResponseStatus(HttpStatus.OK)
    public List<EventFullDto> getEventsForModeration(
            @RequestParam(defaultValue = "0") Integer from,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        log.info("Получен запрос на получение списка событий для модерации. Параметры: from={}, size={}", from, size);

        List<EventFullDto> events = adminEventService.getEventsForModeration(from, size);

        log.info("Получен список событий для модерации. Количество элементов: {}", events.size());

        return events;
    }
}
