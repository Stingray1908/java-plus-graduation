package ru.yandex.practicum.event.service;

import ru.yandex.practicum.dto.events.*;
import ru.yandex.practicum.enums.EventState;
import ru.yandex.practicum.enums.EventsSortType;
import ru.yandex.practicum.error.exception.ForbiddenActionException;
import ru.yandex.practicum.error.exception.NotFoundException;

import java.time.LocalDateTime;
import java.util.List;

public interface EventsService {

    List<EventShortDto> getPublishedEvents(
            String text,
            List<Long> categoryIds,
            Boolean paid,
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd,
            Boolean onlyAvailable,
            EventsSortType sort,
            int from,
            int size
    );

    EventFullDto getPublishedEventById(Long id);

    List<EventFullDto> getEvents(
            List<Long> userIds,
            List<String> states,
            List<Long> categoryIds,
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd,
            int from,
            int size
    );

    EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest request);

    /**
     * Сохраняет новое событие, инициированное пользователем.
     *
     * @param newEventDto DTO с данными нового события
     * @param userId      ID пользователя, создающего событие
     * @return DTO полного представления сохранённого события
     */
    EventFullDto saveEvent(NewEventDto newEventDto, Long userId);


    /**
     * Обновляет данные события, если оно находится в состоянии «отменено» или «ожидает модерации».
     *
     * @param userId                 ID пользователя, инициирующего обновление
     * @param eventId                ID события, которое требуется обновить
     * @param updateEventUserRequest DTO с данными для обновления события (поля могут быть null)
     * @return DTO полного представления обновлённого события
     * @throws NotFoundException        если событие с указанным ID не найдено
     * @throws ForbiddenActionException если обновление запрещено (неверный статус или дата слишком ранняя)
     */
    EventFullDto updateInactiveEvent(Long userId, Long eventId, UpdateEventUserRequest updateEventUserRequest);

    List<EventFullDto> getUserEvents(Long userId, int from, int size);

    List<EventFullDto> getUserModerationHistory(Long userId, int from, int size);

    EventFullDto getUserEventById(Long userId, Long eventId);

    List<EventFullDto> getEventsForModeration(int from, int size);

    /**
     * Получает актуальные опубликованные события по ID подписчика.
     *
     * @param subscriberId ID подписчика (пользователя, на чьи подписки ориентируемся)
     * @param state        Требуемый статус события (обычно PUBLISHED)
     * @param now          Текущая дата/время (для фильтрации событий, которые ещё не прошли)
     * @param from         Смещение (offset) для пагинации
     * @param size         Количество записей (limit) для пагинации
     * @return Список DTO полных событий
     */
    List<EventFullDto> findActualPublishedEventsBySubscriberId(
            Long subscriberId,
            EventState state,
            LocalDateTime now,
            int from,
            int size
    );

}
