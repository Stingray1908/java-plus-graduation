package ru.yandex.practicum.event.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.StatsClient;
import ru.yandex.practicum.categories.service.CategoryService;
import ru.yandex.practicum.categories.service.CategoryServiceImpl;
import ru.yandex.practicum.dto.ViewStats;
import ru.yandex.practicum.dto.categories.CategoryDto;
import ru.yandex.practicum.dto.events.*;
import ru.yandex.practicum.dto.events.moderation.ModerationCommentShortDto;
import ru.yandex.practicum.dto.user.UserDto;
import ru.yandex.practicum.dto.user.UserShortDto;
import ru.yandex.practicum.enums.EventState;
import ru.yandex.practicum.enums.EventsSortType;
import ru.yandex.practicum.enums.StateAction;
import ru.yandex.practicum.error.exception.ConflictException;
import ru.yandex.practicum.error.exception.EventCreationRuleException;
import ru.yandex.practicum.error.exception.ForbiddenActionException;
import ru.yandex.practicum.error.exception.NotFoundException;
import ru.yandex.practicum.event.entity.Event;
import ru.yandex.practicum.event.mapper.EventsMapper;
import ru.yandex.practicum.event.moderation.ModerationComment;
import ru.yandex.practicum.event.moderation.ModerationCommentRepository;
import ru.yandex.practicum.event.moderation.ModerationService;
import ru.yandex.practicum.event.repo.EventsRepository;
import ru.yandex.practicum.feigns.request.RequestAdditionalFeign;
import ru.yandex.practicum.feigns.user.UserAdminFeign;
import ru.yandex.practicum.rating.service.RateServiceImpl;
import ru.yandex.practicum.subscriptions.SubscriptionRepository;
import ru.yandex.practicum.subscriptions.SubscriptionServiceImpl;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static ru.yandex.practicum.event.mapper.EventsMapper.*;
import static ru.yandex.practicum.event.moderation.ModerationMapper.toModerationCommentShortDto;

@Service
@Transactional
@Slf4j
public class EventsServiceImpl implements EventsService {
    private static final int MIN_HOURS_BEFORE_EVENT = 2;
    private final SubscriptionRepository subscriptionRepository;
    private final UserAdminFeign userAdminFeign;
    private final CategoryService categoryService;
    private final EventsRepository eventRepository;
    private final StatsClient statsClient;
    private final EntityManager entityManager;
    private final RequestAdditionalFeign requestAdditionalFeign;
    private final ModerationService moderationService;
    private final RateServiceImpl rateService;

    public EventsServiceImpl(SubscriptionRepository subscriptionRepository,
                             UserAdminFeign userAdminFeign,
                             CategoryService categoryService,
                             EventsRepository eventRepository,
                             @Qualifier("StatsClientDiscovery") StatsClient statsClient,
                             EntityManager entityManager,
                             RequestAdditionalFeign requestAdditionalFeign,
                             ModerationService moderationService,
                             RateServiceImpl rateService) {
        this.subscriptionRepository = subscriptionRepository;
        this.userAdminFeign = userAdminFeign;
        this.categoryService = categoryService;
        this.eventRepository = eventRepository;
        this.statsClient = statsClient;
        this.entityManager = entityManager;
        this.requestAdditionalFeign = requestAdditionalFeign;
        this.moderationService = moderationService;
        this.rateService = rateService;
    }

    @Override
    public EventFullDto findEventById(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Event with id=" + id + " was not found"));

        return toEventFullDto(event,
                getUserById(event.getInitiatorId()),
                categoryService.getCategoryById(event.getCategoryId()),
                moderationService.getCommentById(id),
                getConfirmedRequestsForEvent(id),
                0L,
                0L);
    }


    @Override
    public List<EventFullDto> getEventsByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        List<Event> events = eventRepository.findAllById(ids);
        if (events.isEmpty()) {
            return List.of();
        }

        return toListEventFullDtos(events,
                getUserMapForEvents(events),
                getCategoryMapForEvents(events),
                moderationService.getCommentsMap(ids),
                getConfirmedRequestsMap(ids),
                Collections.emptyMap(),
                Collections.emptyMap());
    }


    @Override
    public List<EventShortDto> getEventShortDtoByIdsWithStats(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> uniqueIds = ids.stream().distinct().toList();
        List<Event> events = eventRepository.findAllById(uniqueIds);
        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        return toListShortEventDtos(
                events,
                getConfirmedRequestsMap(uniqueIds),
                getUserMapForEvents(events),
                getCategoryMapForEvents(events),
                getRatingsMap(uniqueIds),
                getViewsMap(uniqueIds));
    }

    @Override
    public EventFullDto saveEvent(NewEventDto newEventDto, Long userId) {
        validateEventDate(newEventDto.getEventDate());

        UserShortDto user = getUserById(userId);
        CategoryDto category = categoryService.getCategoryById(newEventDto.getCategory());
        Event event = toEvent(newEventDto, userId, category.getId());

        eventRepository.save(event);

        return toEventFullDto(event,
                user,
                category,
                null,
                getConfirmedRequestsForEvent(event.getId()),
                0L,
                0L);
    }

    @Override
    public List<EventShortDto> getPublishedEvents(
            String text,
            List<Long> categoryIds,
            Boolean paid,
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd,
            Boolean onlyAvailable,
            EventsSortType sort,
            int from,
            int size) {

        Pageable pageable = PageRequest.of(from / size, size);
        if (rangeStart == null) rangeStart = LocalDateTime.now();

        Specification<Event> spec = Specification.where(EventSpecification.hasStatePublished())
                .and(EventSpecification.hasTextInAnnotationOrDescription(text))
                .and(EventSpecification.belongsToCategories(categoryIds))
                .and(EventSpecification.isPaid(paid))
                .and(EventSpecification.isWithinRange(rangeStart, rangeEnd));

        List<Event> events = eventRepository.findAll(spec, pageable).getContent();

        if (Boolean.TRUE.equals(onlyAvailable)) {
            events.removeIf(event -> event.getParticipantLimit() > 0 &&
                    event.getConfirmedRequests() >= event.getParticipantLimit());
        }

        List<Long> eventIds = events.stream().map(Event::getId).toList();
        List<EventShortDto> dtoList = getEventShortDtoByIdsWithStats(eventIds);

        if (sort == EventsSortType.VIEWS) {
            dtoList.sort((e1, e2) -> Long.compare(e2.getViews(), e1.getViews()));
        } else if (sort == EventsSortType.RATING) {
            dtoList.sort((e1, e2) -> Long.compare(e2.getRating(), e1.getRating()));
        }

        return dtoList;
    }

    @Override
    public EventFullDto getPublishedEventById(Long id) {
        Event event = eventRepository.findByIdAndState(id, EventState.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Event with id=" + id + " was not found"));

        return toEventFullDto(event,
                getUserById(event.getInitiatorId()),
                categoryService.getCategoryById(event.getCategoryId()),
                moderationService.getCommentById(id),
                getConfirmedRequestsForEvent(id),
                getRatingForEvents(List.of(id)),
                getViewsMap(List.of(id)).get(id));
    }

    @Override
    public List<EventFullDto> getEvents(
            List<Long> userIds,
            List<String> states,
            List<Long> categoryIds,
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd,
            int from,
            int size
    ) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Event> query = cb.createQuery(Event.class);
        Root<Event> root = query.from(Event.class);

        List<Predicate> predicates = new ArrayList<>();

        if (userIds != null && !userIds.isEmpty()
                && !(userIds.size() == 1 && userIds.get(0) == 0L)) {
            predicates.add(root.get("initiatorId").in(userIds));
        }

        if (states != null && !states.isEmpty()) {
            List<EventState> stateEnums = states.stream()
                    .map(EventState::valueOf)
                    .collect(Collectors.toList());
            predicates.add(root.get("state").in(stateEnums));
        }

        if (categoryIds != null && !categoryIds.isEmpty()
                && !(categoryIds.size() == 1 && categoryIds.get(0) == 0L)) {
            predicates.add(root.get("categoryId").in(categoryIds));
        }

        if (rangeStart != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("eventDate"), rangeStart));
        }
        if (rangeEnd != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("eventDate"), rangeEnd));
        }

        query.select(root).where(predicates.toArray(new Predicate[0]));
        query.orderBy(cb.desc(root.get("eventDate")));

        PageRequest pageRequest = PageRequest.of(from / size, size);
        List<Event> events = entityManager.createQuery(query)
                .setFirstResult((int) pageRequest.getOffset())
                .setMaxResults(pageRequest.getPageSize())
                .getResultList();

        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> ids = events.stream().map(Event::getId).toList();
        return toListEventFullDtos(events,
                getUserMapForEvents(events),
                getCategoryMapForEvents(events),
                Collections.emptyMap(),
                getConfirmedRequestsMap(ids),
                getRatingsMap(ids),
                Collections.emptyMap());
    }


    @Override
    public EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        ModerationComment moderationComment;

        if (request.getStateAction() != null) {
            switch (request.getStateAction()) {
                case PUBLISH_EVENT -> {
                    if (!event.getState().equals(EventState.PENDING)) {
                        throw new ConflictException("Cannot publish event in state: " + event.getState());
                    }
                    if (event.getEventDate().isBefore(LocalDateTime.now().plusHours(1))) {
                        throw new ConflictException("Event must be at least 1 hour after current time to be published");
                    }
                    event.setState(EventState.PUBLISHED);
                    event.setPublishedOn(LocalDateTime.now());
                }
                case REJECT_EVENT -> {
                    if (event.getState().equals(EventState.PUBLISHED)) {
                        throw new ConflictException("Cannot reject published event");
                    }

                    if (request.getModerationComment() != null && !request.getModerationComment().trim().isEmpty()) {
                        moderationComment = ModerationComment.builder()
                                .event(event)
                                .commentText(request.getModerationComment())
                                .createdOn(LocalDateTime.now())
                                .build();
                        moderationComment = moderationService.save(moderationComment);
                    }

                    event.setState(EventState.CANCELED);
                    event.setRequestModeration(false);
                }
            }
        }

        applyNonNullUpdates(event, request);

        eventRepository.save(event);
        return toEventFullDto(event,
                getUserById(event.getInitiatorId()),
                categoryService.getCategoryById(event.getCategoryId()),
                moderationService.getCommentById(eventId),
                getConfirmedRequestsForEvent(eventId),
                getRatingForEvents(List.of(eventId)),
                getViewsMap(List.of(eventId)).get(eventId));
    }

    @Override
    @Transactional
    public EventFullDto updateInactiveEvent(Long userId, Long eventId, UpdateEventUserRequest updateEventUserRequest) {
        log.info("Начало обновления события с ID: {} для пользователя с ID: {}", eventId, userId);
        log.debug("Dto {}", updateEventUserRequest);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с ID " + eventId + " не найдено"));

        if (!event.getInitiatorId().equals(userId)) {
            throw new ForbiddenActionException("Пользователь с ID " + userId + " не является инициатором события " + eventId);
        }

        EventState currentState = event.getState();
        if (!currentState.equals(EventState.CANCELED) && !currentState.equals(EventState.PENDING)) {
            throw new ConflictException(
                    "Только отменённые события или события в состоянии ожидания модерации могут быть изменены. Текущий статус: " + currentState
            );
        }

        StateAction stateAction = updateEventUserRequest.getStateAction();
        if (stateAction != null) {
            switch (stateAction) {
                case SEND_TO_REVIEW:
                    event.setState(EventState.PENDING);
                    break;
                case CANCEL_REVIEW:
                    event.setState(EventState.CANCELED);
                    break;
                default:
                    throw new ConflictException(
                            "Недопустимое значение stateAction: " + stateAction +
                                    ". Допустимые значения: SEND_TO_REVIEW, CANCEL_REVIEW"
                    );
            }
        }

        applyNonNullUpdates(event, updateEventUserRequest);

        LocalDateTime updateDate = updateEventUserRequest.getEventDate();
        if (updateDate != null) {
            validateEventDate(updateDate);
        } else if (stateAction == StateAction.SEND_TO_REVIEW) {
            validateEventDate(event.getEventDate());
            event.setRequestModeration(true);
        }

        eventRepository.save(event);
        log.info("Событие с ID: {} успешно обновлено", eventId);

        return toEventFullDto(event,
                getUserById(userId),
                categoryService.getCategoryById(event.getCategoryId()),
                moderationService.getCommentById(eventId),
                getConfirmedRequestsForEvent(eventId),
                getRatingForEvents(List.of(eventId)),
                0L);
    }

    @Override
    public List<EventFullDto> getUserEvents(Long userId, int from, int size) {
        log.debug("Начинаем поиск событий для пользователя с ID: {}, from: {}, size: {}", userId, from, size);

        UserShortDto user = getUserById(userId);
        List<Event> events = eventRepository.findAllByInitiatorIdWithOffset(userId, from, size);

        if (events.isEmpty()) {
            log.debug("Для пользователя с ID {} не найдено событий", userId);
            return Collections.emptyList();
        }

        List<Long> ids = events.stream().map(Event::getId).toList();

        List<EventFullDto> eventFullDtoList = toListEventFullDtos(
                events,
                Collections.emptyMap(),
                getCategoryMapForEvents(events),
                Collections.emptyMap(),
                getConfirmedRequestsMap(ids),
                getRatingsMap(ids),
                Collections.emptyMap()
        );

        eventFullDtoList.forEach(e -> e.setInitiator(user));
        return eventFullDtoList;
    }

    @Override
    public EventFullDto getUserEventById(Long userId, Long eventId) {
        log.debug("Начинаем поиск события с ID: {} для пользователя с ID: {}", eventId, userId);

        UserShortDto user = getUserById(userId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (!event.getInitiatorId().equals(userId)) {
            throw new ForbiddenActionException(
                    "Пользователь с ID " + userId + " не является инициатором события " + eventId
            );
        }

        log.debug("Событие найдено в БД: ID {}, заголовок '{}'", event.getId(), event.getTitle());

        return toEventFullDto(event,
                user,
                categoryService.getCategoryById(event.getCategoryId()),
                null,
                getConfirmedRequestsForEvent(eventId),
                getRatingForEvents(List.of(eventId)),
                getViewsMap(List.of(eventId)).get(eventId));
    }

    private void validateEventDate(LocalDateTime eventDate) {
        LocalDateTime minEventDate = LocalDateTime.now().plusHours(MIN_HOURS_BEFORE_EVENT);
        if (eventDate.isBefore(minEventDate)) {
            throw new EventCreationRuleException("eventDate", eventDate, "Событие не удовлетворяет правилам создания");
        }
    }

    private Map<Long, Long> getConfirmedRequestsMap(List<Long> eventIds) {
        if (eventIds.isEmpty()) return Map.of();

        return requestAdditionalFeign.countRequestsByEventIdsAndStatus(eventIds, EventState.CONFIRMED).stream()
                .collect(Collectors.toMap(
                        r -> ((Number) r[0]).longValue(),
                        r -> ((Number) r[1]).longValue()
                ));
    }

    private Long getConfirmedRequestsForEvent(Long eventId) {
        if (eventId == null) {
            return 0L;
        }
        Map<Long, Long> result = getConfirmedRequestsMap(List.of(eventId));

        return result.get(eventId);
    }

    private <T extends UpdateEventRequest> void applyNonNullUpdates(Event event, T request) {
        if (request.getAnnotation() != null) event.setAnnotation(request.getAnnotation());
        if (request.getDescription() != null) event.setDescription(request.getDescription());
        if (request.getTitle() != null) event.setTitle(request.getTitle());
        if (request.getPaid() != null) event.setPaid(request.getPaid());
        if (request.getParticipantLimit() != null) event.setParticipantLimit(request.getParticipantLimit());
        if (request.getRequestModeration() != null) event.setRequestModeration(request.getRequestModeration());
        if (request.getLocation() != null) {
            event.setLocationLat(request.getLocation().getLat());
            event.setLocationLon(request.getLocation().getLon());
        }
        if (request.getCategory() != null) {
            CategoryDto category = categoryService.getCategoryById(request.getCategory());

            event.setCategoryId(category.getId());
        }
        if (request.getEventDate() != null) event.setEventDate(request.getEventDate());
    }

    @Override
    public List<EventFullDto> getUserModerationHistory(Long userId, int page, int limit) {

        Pageable pageable = PageRequest.of(page, limit);
        UserShortDto user = getUserById(userId);
        List<Event> events = eventRepository.findUserModerationHistory(userId, pageable);

        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .collect(Collectors.toList());

        List<EventFullDto> dtos = toListEventFullDtos(events,
                Collections.emptyMap(),
                getCategoryMapForEvents(events),
                moderationService.getCommentsMap(eventIds),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap());

        dtos.forEach(e -> e.setInitiator(user));
        return dtos;
    }

    @Override
    public List<EventFullDto> getEventsForModeration(int page, int limit) {
        Pageable pageable = PageRequest.of(page, limit);
        List<Event> events = eventRepository.findByRequestModerationAndState(
                Boolean.TRUE, EventState.PENDING, pageable
        );

        return toListEventFullDtos(events,
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap());
    }

    private UserShortDto getUserById(Long id) {
        UserShortDto user = userAdminFeign.getById(id);
        if (user == null) {
            throw new NotFoundException("пользователь не найден");
        }
        return user;
    }

    @Override
    public List<EventFullDto> findActualPublishedEventsBySubscriberId(
            Long subscriberId,
            EventState state,
            LocalDateTime now,
            int from,
            int size) {

        log.debug("Запрос актуальных опубликованных событий: subscriberId={}, state={}, now={}, from={}, size={}",
                subscriberId, state, now, from, size);

        var pageable = PageRequest.of(from / size, size);

        List<Long> publishers = subscriptionRepository.findPublishersBySubscriber(subscriberId);

        if (publishers == null || publishers.isEmpty()) {
            log.debug("У подписчика ID={} нет активных подписок", subscriberId);
            return Collections.emptyList();
        }

        List<Event> events = eventRepository.findEventsByPublisherIdsAndStatusAndTimeAfter(
                publishers, state, now, pageable);

        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        log.debug("Найдено {} событий для подписчика ID={}", events.size(), subscriberId);
        return toListEventFullDtos(events,
                getUserMapForEvents(events),
                getCategoryMapForEvents(events),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap());
    }

    public Long getRatingForEvents(List<Long> ids) {
        List<Object[]> rating = rateService.getRatingsForEvents(ids);

        return rating.isEmpty() ? 0L : (Long) rating.getFirst()[1];
    }

    private Map<Long, Long> getViewsMap(List<Long> events) {
        if (events.isEmpty()) return Map.of();

        List<String> uris = events.stream()
                .map(e -> "/events/" + e)
                .collect(toList());

        LocalDateTime start = LocalDateTime.now().minusMonths(1);
        LocalDateTime end = LocalDateTime.now();

        List<ViewStats> stats;
        try {
            stats = statsClient.getStats(start, end, uris, true);
        } catch (Exception e) {
            log.error("Ошибка при получении статистики", e);
            return Map.of();
        }

        Map<Long, Long> viewsMap = new HashMap<>();
        for (ViewStats stat : stats) {
            String uri = stat.getUri();
            if (uri.startsWith("/events/")) {
                try {
                    Long eventId = Long.parseLong(uri.substring("/events/".length()));
                    viewsMap.put(eventId, stat.getHits());
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return viewsMap;
    }

    private Map<Long, Long> getRatingsMap(List<Long> eventIds) {
        if (eventIds.isEmpty()) return Map.of();

        List<Object[]> results = rateService.getRatingsForEvents(eventIds);
        return results.stream().collect(Collectors.toMap(
                row -> ((Number) row[0]).longValue(),
                row -> ((Number) row[1]).longValue()
        ));
    }

    private Map<Long, UserShortDto> getUserMapForEvents(List<Event> events) {
        List<Long> userIds = events.stream()
                .map(Event::getInitiatorId)
                .distinct()
                .toList();
        if (userIds.isEmpty()) return Map.of();
        return userAdminFeign.getAllInIds(userIds).stream()
                .collect(Collectors.toMap(UserShortDto::getId, Function.identity()));
    }

    private Map<Long, CategoryDto> getCategoryMapForEvents(List<Event> events) {
        List<Long> categoryIds = events.stream()
                .map(Event::getCategoryId)
                .distinct()
                .toList();
        if (categoryIds.isEmpty()) return Map.of();
        return categoryIds.stream()
                .collect(Collectors.toMap(
                        id -> id,
                        categoryService::getCategoryById
                ));
    }
}
