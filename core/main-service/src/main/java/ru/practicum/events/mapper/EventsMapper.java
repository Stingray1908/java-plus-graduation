package ru.practicum.events.mapper;

import ru.practicum.categories.entity.Category;
import ru.yandex.practicum.enums.EventState;
import ru.yandex.practicum.dto.events.Location;
import ru.yandex.practicum.dto.events.EventFullDto;
import ru.yandex.practicum.dto.events.EventShortDto;
import ru.yandex.practicum.dto.events.NewEventDto;
import ru.practicum.events.entity.Event;
import ru.practicum.events.moderation.ModerationComment;
import ru.practicum.user.User;
import ru.practicum.user.UserMapper;

import java.time.LocalDateTime;

import static ru.practicum.categories.mapper.CategoryMapper.toCategoryDto;
import static ru.yandex.practicum.Constance.FORMATTER;
import static ru.practicum.events.moderation.ModerationMapper.moderationCommentShortDto;

public class EventsMapper {

    public static EventShortDto toShortEventDto(Event event, Long confirmedRequests) {
        EventShortDto dto = new EventShortDto();
        dto.setId(event.getId());
        dto.setAnnotation(event.getAnnotation());
        dto.setCategory(toCategoryDto(event.getCategory()));
        dto.setConfirmedRequests(confirmedRequests);
        dto.setEventDate(event.getEventDate().format(FORMATTER));
        dto.setInitiator(new UserMapper().toShortDto(event.getInitiator()));
        dto.setPaid(event.getPaid());
        dto.setTitle(event.getTitle());
        dto.setViews(event.getViews());
        return dto;
    }

    public static EventShortDto toShortEventDto(Event event, Long confirmedRequests, Long rating) {
        EventShortDto dto = toShortEventDto(event, confirmedRequests);
        dto.setRating(rating != null ? rating : 0L);
        return dto;
    }

    public static EventFullDto toEventFullDto(Event event, Long rating) {
        EventFullDto dto = toEventFullDto(event);
        dto.setRating(rating != null ? rating : 0L);
        return dto;
    }

    public static EventFullDto toEventFullDto(Event event) {
        EventFullDto dto = new EventFullDto();
        dto.setId(event.getId());
        dto.setAnnotation(event.getAnnotation());
        dto.setCategory(toCategoryDto(event.getCategory()));
        dto.setConfirmedRequests(event.getConfirmedRequests());
        dto.setCreatedOn(format(event.getCreatedOn()));
        dto.setDescription(event.getDescription());
        dto.setEventDate(event.getEventDate().format(FORMATTER));
        dto.setInitiator(new UserMapper().toShortDto(event.getInitiator()));
        dto.setLocation(new Location(event.getLocationLat(), event.getLocationLon()));
        dto.setPaid(event.getPaid());
        dto.setParticipantLimit(event.getParticipantLimit());
        dto.setPublishedOn(event.getPublishedOn() != null ? format(event.getPublishedOn()) : null);
        dto.setRequestModeration(event.getRequestModeration());
        dto.setState(event.getState().name());
        dto.setTitle(event.getTitle());
        dto.setViews(event.getViews());
        return dto;
    }

    public static EventFullDto toEventFullDto(Event event, ModerationComment mc, Long rating) {
        EventFullDto dto = toEventFullDto(event);
        dto.setRating(rating != null ? rating : 0L);
        if (mc != null) {
            dto.setLastModerationCommentDto(moderationCommentShortDto(mc));
        }
        return dto;
    }

    public static EventFullDto toEventFullDto(Event event, ModerationComment mc) {
        EventFullDto dto = toEventFullDto(event);
        if (mc != null) {
            dto.setLastModerationCommentDto(moderationCommentShortDto(mc));
        }
        return dto;
    }

    /**
     * Преобразует DTO нового события в сущность Event.
     *
     * @param dto  DTO с данными нового события
     * @param user пользователь-инициатор события
     * @return сущность Event, готовая для сохранения в БД
     */
    public static Event toEvent(NewEventDto dto, User user, Category category) {
        return Event.builder()
                .annotation(dto.getAnnotation())
                .category(category)
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
                .initiator(user)
                .confirmedRequests(0L)
                .views(0L)
                .build();
    }

    private static String format(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(FORMATTER) : null;
    }
}
