package ru.yandex.practicum.event.mapper;

import lombok.AllArgsConstructor;
import ru.yandex.practicum.categories.repo.CategoryRepository;
import ru.yandex.practicum.dto.events.EventFullDto;
import ru.yandex.practicum.dto.events.EventShortDto;
import ru.yandex.practicum.dto.events.Location;
import ru.yandex.practicum.dto.events.NewEventDto;
import ru.yandex.practicum.dto.user.UserDto;
import ru.yandex.practicum.dto.user.UserShortDto;
import ru.yandex.practicum.enums.EventState;
import ru.yandex.practicum.event.entity.Event;
import ru.yandex.practicum.event.moderation.ModerationComment;
import ru.yandex.practicum.feigns.user.UserAdminFeign;

import java.time.LocalDateTime;

import static ru.yandex.practicum.Constance.FORMATTER;
import static ru.yandex.practicum.categories.mapper.CategoryMapper.toCategoryDto;
import static ru.yandex.practicum.event.moderation.ModerationMapper.moderationCommentShortDto;

@AllArgsConstructor
public class EventsMapper {



    public static EventShortDto toShortEventDto(Event event, Long confirmedRequests) {
        //UserDto user = userAdminFeign.getById(event.getInitiatorId());
        EventShortDto dto = new EventShortDto();
        dto.setId(event.getId());
        dto.setAnnotation(event.getAnnotation());
        dto.setConfirmedRequests(confirmedRequests);
        dto.setEventDate(event.getEventDate().format(FORMATTER));
        dto.setPaid(event.getPaid());
        dto.setTitle(event.getTitle());
        //dto.setCategory(toCategoryDto(categoryRepository.getReferenceById(event.getId())));
        //dto.setInitiator(new UserShortDto(user.getId(), user.getName()));


        return dto;
    }

    public static EventShortDto toShortEventDto(Event event, Long confirmedRequests, Long rating, Long views) {
        EventShortDto dto = toShortEventDto(event, confirmedRequests);
        dto.setRating(rating != null ? rating : 0L);
        dto.setViews(views != null ? views : 0L);
        return dto;
    }

    public static EventFullDto toEventFullDto(Event event, Long rating) {
        EventFullDto dto = toEventFullDto(event);
        dto.setRating(rating != null ? rating : 0L);
        return dto;
    }

    public static EventFullDto toEventFullDto(Event event) {
        EventFullDto dto = new EventFullDto();
        //UserDto user = userAdminFeign.getById(event.getInitiatorId());
        dto.setId(event.getId());
        dto.setAnnotation(event.getAnnotation());
        //dto.setCategory(toCategoryDto(categoryRepository.getReferenceById(event.getCategoryId())));
        dto.setConfirmedRequests(event.getConfirmedRequests());
        dto.setCreatedOn(format(event.getCreatedOn()));
        dto.setDescription(event.getDescription());
        dto.setEventDate(event.getEventDate().format(FORMATTER));
        //dto.setInitiator(new UserShortDto(user.getId(), user.getName()));
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
