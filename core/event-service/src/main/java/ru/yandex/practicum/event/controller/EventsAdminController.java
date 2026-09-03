package ru.yandex.practicum.event.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.dto.events.EventFullDto;
import ru.yandex.practicum.dto.events.EventShortDto;
import ru.yandex.practicum.dto.events.UpdateEventAdminRequest;
import ru.yandex.practicum.feigns.event.EventsAdminFeign;
import ru.yandex.practicum.event.service.EventAdditionalService;
import ru.yandex.practicum.event.service.EventsService;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/admin/events")
@RequiredArgsConstructor
public class EventsAdminController implements EventsAdminFeign {

    private final EventsService adminEventService;
    private final EventAdditionalService eventAdditionalService;

    @Override
    public ResponseEntity<EventFullDto> getEventById(@PathVariable Long eventId){
        EventFullDto dto = eventAdditionalService.findEventById(eventId);
        return ResponseEntity.ok(dto);
    }

    @Override
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

    @Override
    public ResponseEntity<EventFullDto> updateEventByAdmin(
            @PathVariable Long eventId,
            @Valid @RequestBody UpdateEventAdminRequest updateRequest
    ) {
        EventFullDto updatedEvent = adminEventService.updateEventByAdmin(eventId, updateRequest);
        return ResponseEntity.ok(updatedEvent);
    }

    @Override
    public List<EventFullDto> getEventsForModeration(
            @RequestParam(defaultValue = "0") Integer from,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        log.info("Получен запрос на получение списка событий для модерации. Параметры: from={}, size={}", from, size);

        List<EventFullDto> events = adminEventService.getEventsForModeration(from, size);

        log.info("Получен список событий для модерации. Количество элементов: {}", events.size());

        return events;
    }

    @Override
    public List<EventFullDto> getEventsByIds(List<Long> ids) {
        return eventAdditionalService.getEventsByIds(ids);
    }

    @Override
    public List<EventShortDto> getEventShortDtoByIdsWithStats(List<Long> ids) {
        return eventAdditionalService.getEventShortDtoByIdsWithStats(ids);
    }
}
