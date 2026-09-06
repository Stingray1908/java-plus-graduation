package ru.yandex.practicum.event.mapper;

import lombok.AllArgsConstructor;
import ru.yandex.practicum.dto.categories.CategoryDto;
import ru.yandex.practicum.dto.events.EventFullDto;
import ru.yandex.practicum.dto.events.EventShortDto;
import ru.yandex.practicum.dto.events.Location;
import ru.yandex.practicum.dto.events.NewEventDto;
import ru.yandex.practicum.dto.events.moderation.ModerationCommentShortDto;
import ru.yandex.practicum.dto.user.UserShortDto;
import ru.yandex.practicum.enums.EventState;
import ru.yandex.practicum.event.entity.Event;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static ru.yandex.practicum.Constants.FORMATTER;

@AllArgsConstructor
public class EventsMapper {

    public static EventShortDto toShortEventDto(Event event,
                                                Long confirmedRequests,
                                                UserShortDto user,
                                                CategoryDto category,
                                                Long rating,
                                                Long views) {
        EventShortDto dto = new EventShortDto();
        dto.setId(event.getId());
        dto.setAnnotation(event.getAnnotation());
        dto.setConfirmedRequests(confirmedRequests);
        dto.setEventDate(event.getEventDate().format(FORMATTER));
        dto.setPaid(event.getPaid());
        dto.setTitle(event.getTitle());
        dto.setInitiator(user);
        dto.setCategory(category);
        dto.setRating(rating != null ? rating : 0L);
        dto.setViews(views != null ? views : 0L);
        return dto;
    }

    public static List<EventShortDto> toListShortEventDtos(List<Event> events,
                                                           Map<Long, Long> confirmedRequests,
                                                           Map<Long, UserShortDto> userMap,
                                                           Map<Long, CategoryDto> categoryMap,
                                                           Map<Long, Long> ratings,
                                                           Map<Long, Long> views) {
        return events.stream()
                .map(e -> toShortEventDto(e,
                        confirmedRequests.getOrDefault(e.getId(), 0L),
                        userMap.get(e.getInitiatorId()),
                        categoryMap.get(e.getCategoryId()),
                        ratings.getOrDefault(e.getId(), 0L),
                        views.getOrDefault(e.getId(), 0L)))
                .toList();
    }

    public static EventFullDto toEventFullDto(Event event,
                                              UserShortDto user,
                                              CategoryDto category,
                                              ModerationCommentShortDto commentShortDto,
                                              Long confirmedRequests,
                                              Long rating,
                                              Long views) {
        Location location = new Location(event.getLocationLat(), event.getLocationLon());

        EventFullDto dto = new EventFullDto();
        dto.setId(event.getId());
        dto.setInitiator(user);
        dto.setCategory(category);
        dto.setLastModerationCommentDto(commentShortDto);
        dto.setAnnotation(event.getAnnotation());
        dto.setConfirmedRequests(confirmedRequests);
        dto.setCreatedOn(format(event.getCreatedOn()));
        dto.setDescription(event.getDescription());
        dto.setEventDate(event.getEventDate().format(FORMATTER));
        dto.setLocation(location);
        dto.setPaid(event.getPaid());
        dto.setParticipantLimit(event.getParticipantLimit());
        dto.setPublishedOn(event.getPublishedOn() != null ? format(event.getPublishedOn()) : null);
        dto.setRequestModeration(event.getRequestModeration());
        dto.setState(event.getState().name());
        dto.setTitle(event.getTitle());
        dto.setRating(rating != null ? rating : 0L);
        dto.setViews(views != null ? views : 0L);

        return dto;
    }

    public static List<EventFullDto> toListEventFullDtos(List<Event> events,
                                                         Map<Long, UserShortDto> userMap,
                                                         Map<Long, CategoryDto> categoryMap,
                                                         Map<Long, ModerationCommentShortDto> commentMap,
                                                         Map<Long, Long> confirmedRequestsMap,
                                                         Map<Long, Long> ratings,
                                                         Map<Long, Long> views) {
        return events.stream()
                .map(e -> toEventFullDto(e,
                        userMap.getOrDefault(e.getInitiatorId(), null),
                        categoryMap.getOrDefault(e.getCategoryId(), null),
                        commentMap.getOrDefault(e.getId(), null),
                        confirmedRequestsMap.getOrDefault(e.getId(), 0L),
                        ratings.getOrDefault(e.getId(), 0L),
                        views.getOrDefault(e.getId(), 0L)))
                .toList();
    }

    /**
     * Преобразует DTO нового события в сущность Event.
     *
     * @param dto DTO с данными нового события
     *            // * @param user пользователь-инициатор события
     * @return сущность Event, готовая для сохранения в БД
     */
    public static Event toEvent(NewEventDto dto, Long user, Long category) {
        return Event.builder()
                .annotation(dto.getAnnotation())
                .categoryId(category)
                .description(dto.getDescription())
                .title(dto.getTitle())
                .eventDate(dto.getEventDate())
                .paid(dto.getPaid())
                .participantLimit(dto.getParticipantLimit())
                .requestModeration(dto.getRequestModeration())
                .locationLat(dto.getLocation().getLat())
                .locationLon(dto.getLocation().getLon())
                .createdOn(LocalDateTime.now())
                .state(EventState.PENDING)
                .initiatorId(user)
                .confirmedRequests(0L)
                .views(0L)
                .build();
    }

    private static String format(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(FORMATTER) : null;
    }
}
