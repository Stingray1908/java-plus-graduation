package ru.yandex.practicum.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.dto.events.EventFullDto;
import ru.yandex.practicum.dto.events.NewEventDto;
import ru.yandex.practicum.dto.events.UpdateEventUserRequest;
import ru.yandex.practicum.feigns.event.EventPrivateFeign;
import ru.yandex.practicum.service.EventsService;

import java.util.List;

@RestController
@RequestMapping("/users/{userId}/events")
@RequiredArgsConstructor
@Slf4j
public class EventPrivateController implements EventPrivateFeign {

    private final EventsService eventsService;

    @Override
    public EventFullDto addEvent(
            @Valid @RequestBody NewEventDto newEventDto,
            @PathVariable @Positive Long userId) {

        log.info("Получен запрос на создание нового события для пользователя с ID: {}. Заголовок события: '{}'", userId, newEventDto.getTitle());
        log.debug("Полные данные события, полученные от клиента: {}", newEventDto);

        EventFullDto savedEvent = eventsService.saveEvent(newEventDto, userId);

        log.info("Событие успешно создано с ID: {} для пользователя с ID: {}", savedEvent.getId(), userId);
        log.debug("Полные данные сохранённого события: {}", savedEvent);

        return savedEvent;
    }

    @Override
    public EventFullDto updateEvent(
            @PathVariable @Positive Long userId,
            @PathVariable @Positive Long eventId,
            @Valid @RequestBody UpdateEventUserRequest updateEventUserRequest) {

        log.info("Получен запрос на обновление события с ID: {} для пользователя с ID: {}", eventId, userId);
        log.debug("Данные для обновления события: {}", updateEventUserRequest);

        EventFullDto updatedEvent = eventsService.updateInactiveEvent(userId, eventId, updateEventUserRequest);

        log.info("Событие с ID: {} успешно обновлено для пользователя с ID: {}", eventId, userId);
        log.debug("Полные данные обновлённого события: {}", updatedEvent);

        return updatedEvent;
    }

    @Override
    public List<EventFullDto> getUserEvents(
            @PathVariable @Positive Long userId,
            @RequestParam(defaultValue = "0") @Min(0) Integer from,
            @RequestParam(defaultValue = "10") @Positive Integer size) {

        log.info("Получен запрос на получение событий пользователя с ID: {}, from: {}, size: {}", userId, from, size);

        List<EventFullDto> userEvents = eventsService.getUserEvents(userId, from, size);

        log.info("Для пользователя с ID {} найдено {} событий", userId, userEvents.size());
        return userEvents;
    }

    @Override
    public List<EventFullDto> getUserModerationHistory(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") Integer from,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        List<EventFullDto> events = eventsService.getUserModerationHistory(userId, from, size);
        return events;
    }

    @Override
    public EventFullDto getUserEventById(
            @PathVariable @Positive Long userId,
            @PathVariable @Positive Long eventId) {


        log.info("Получен запрос на получение события с ID: {} для пользователя с ID: {}", eventId, userId);

        EventFullDto event = eventsService.getUserEventById(userId, eventId);

        log.info("Событие с ID: {} успешно найдено для пользователя с ID: {}", eventId, userId);
        log.debug("Полные данные найденного события: {}", event);

        return event;
    }


}
