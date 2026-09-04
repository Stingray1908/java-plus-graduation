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
import ru.yandex.practicum.categories.service.CategoryServiceImpl;
import ru.yandex.practicum.dto.ViewStats;
import ru.yandex.practicum.dto.categories.CategoryDto;
import ru.yandex.practicum.dto.events.*;
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

@Service
@Transactional
@Slf4j
public class EventsServiceImpl implements EventsService {
    private static final int MIN_HOURS_BEFORE_EVENT = 2;
    private final SubscriptionRepository subscriptionRepository;
    private final UserAdminFeign userAdminFeign;
    private final CategoryServiceImpl categoryService;
    private final EventsRepository eventRepository;
    private final StatsClient statsClient;
    private final EntityManager entityManager;
    private final RequestAdditionalFeign requestAdditionalFeign;
    private final ModerationCommentRepository moderationCommentRepository;
    private final RateServiceImpl rateService;

    public EventsServiceImpl(SubscriptionRepository subscriptionRepository,
                             UserAdminFeign userAdminFeign,
                             CategoryServiceImpl categoryService,
                             EventsRepository eventRepository,
                             @Qualifier("StatsClientDiscovery") StatsClient statsClient,
                             EntityManager entityManager,
                             RequestAdditionalFeign requestAdditionalFeign,
                             ModerationCommentRepository moderationCommentRepository,
                             RateServiceImpl rateService) {
        this.subscriptionRepository = subscriptionRepository;
        this.userAdminFeign = userAdminFeign;
        this.categoryService = categoryService;
        this.eventRepository = eventRepository;
        this.statsClient = statsClient;
        this.entityManager = entityManager;
        this.requestAdditionalFeign = requestAdditionalFeign;
        this.moderationCommentRepository = moderationCommentRepository;
        this.rateService = rateService;
    }

    @Override
    public EventFullDto findEventById(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Event with id=" + id + " was not found"));

        EventFullDto dto = toEventFullDto(event);

        enrichFullEventDtoWithUserAndCategory(
                dto,
                event,
                getUserMap(List.of(event)),
                getCategoryMap(List.of(event))
        );

        return dto;
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

        Map<Long, UserDto> userMap = getUserMap(events);
        Map<Long, CategoryDto> categoryMap = getCategoryMap(events);

        return events.stream()
                .map(event -> {
                    EventFullDto dto = EventsMapper.toEventFullDto(event);
                    enrichFullEventDtoWithUserAndCategory(dto, event, userMap, categoryMap);
                    return dto;
                })
                .toList();
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

        Map<Long, UserDto> userMap = getUserMap(events);
        Map<Long, CategoryDto> categoryMap = getCategoryMap(events);
        Map<Long, Long> confirmedRequests = getConfirmedRequestsMap(uniqueIds);
        Map<Long, Long> views = getViewsMap(uniqueIds);
        Map<Long, Long> ratings = getRatingsMap(uniqueIds);

        return events.stream()
                .map(event -> {
                    EventShortDto dto = toShortEventDto(
                            event,
                            confirmedRequests.getOrDefault(event.getId(), 0L),
                            ratings.getOrDefault(event.getId(), 0L),
                            views.getOrDefault(event.getId(), 0L)
                    );
                    enrichShortEventDtoWithUserAndCategory(dto, event, userMap, categoryMap);
                    return dto;
                })
                .toList();
    }

    @Override
    public EventFullDto saveEvent(NewEventDto newEventDto, Long userId) {
        validateEventDate(newEventDto.getEventDate());

        // 1. Получаем зависимости (пользователь и категория)
        UserDto user = getUserById(userId);
        CategoryDto category = getCategoryById(newEventDto.getCategory());

        Event event = toEvent(newEventDto, userId, category.getId());

        event.setInitiatorId(userId);
        event.setCategoryId(category.getId());
        Event savedEvent = eventRepository.save(event);

        Map<Long, UserDto> userMap = Map.of(userId, user);
        Map<Long, CategoryDto> categoryMap = Map.of(category.getId(), category);

        EventFullDto dto = EventsMapper.toEventFullDto(savedEvent);
        enrichFullEventDtoWithUserAndCategory(dto, savedEvent, userMap, categoryMap);

         dto.setViews(0L);
        dto.setRating(0L);

        return dto;
    }


    @Override
    public List<EventShortDto> getPublishedEvents(
            String text, List<Long> categoryIds, Boolean paid,
            LocalDateTime rangeStart, LocalDateTime rangeEnd,
            Boolean onlyAvailable, EventsSortType sort, int from, int size) {

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

        List<Event> eventsList = List.of(event);
        Map<Long, UserDto> userMap = getUserMap(eventsList);
        Map<Long, CategoryDto> categoryMap = getCategoryMap(eventsList);

        Map<Long, Long> viewsMap = getViewsMap(List.of(id));
        Long views = viewsMap.getOrDefault(id, 0L);

        Map<Long, Long> ratingsMap = getRatingsMap(List.of(id));
        Long rating = ratingsMap.getOrDefault(id, 0L);

        List<Object[]> confirmedRequests = requestAdditionalFeign.countRequestsByEventIdsAndStatus(
                List.of(id), EventState.CONFIRMED
        );
        Long confirmedRequestsCount = confirmedRequests.isEmpty()
                ? 0L
                : (Long) confirmedRequests.getFirst()[1];

        EventFullDto dto = EventsMapper.toEventFullDto(event);

        enrichFullEventDtoWithUserAndCategory(dto, event, userMap, categoryMap);

        dto.setViews(views);
        dto.setRating(rating);
        dto.setConfirmedRequests(confirmedRequestsCount);

        return dto;
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
        Map<Long, Long> confirmedRequests = getConfirmedRequestsMap(ids);
        Map<Long, Long> ratings = getRatingsMap(ids);
        setViewsToEvents(events);

        Map<Long, UserDto> userMap = getUserMap(events);
        Map<Long, CategoryDto> categoryMap = getCategoryMap(events);

        return events.stream()
                .peek(event -> event.setConfirmedRequests(
                        confirmedRequests.getOrDefault(event.getId(), 0L)))
                .map(event -> {
                    EventFullDto dto = toEventFullDto(
                            event, ratings.getOrDefault(event.getId(), 0L));
                    enrichFullEventDtoWithUserAndCategory(dto, event, userMap, categoryMap);
                    return dto;
                })
                .collect(Collectors.toList());
    }


    @Override
    public EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        ModerationComment moderationComment = null;

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
                        moderationComment = moderationCommentRepository.save(moderationComment);
                    }

                    event.setState(EventState.CANCELED);
                    event.setRequestModeration(false);
                }
            }
        }

        applyNonNullUpdates(event, request);

        Event saved = eventRepository.save(event);
        saved.setConfirmedRequests(getConfirmedRequests(List.of(event.getId())));
        setViewsToEvent(saved);

        Long rating = getRatingForEvents(List.of(saved.getId()));
        EventFullDto dto = EventsMapper.toEventFullDto(saved, moderationComment, rating);

        enrichFullEventDtoWithUserAndCategory(
                dto,
                event,
                getUserMap(List.of(saved)),
                getCategoryMap(List.of(saved))
        );
        return dto;
    }

    @Override
    @Transactional
    public EventFullDto updateInactiveEvent(Long userId, Long eventId, UpdateEventUserRequest updateEventUserRequest) {
        log.info("Начало обновления события с ID: {} для пользователя с ID: {}", eventId, userId);
        log.debug("Dto {}", updateEventUserRequest);

        // 1. Находим событие по ID
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с ID " + eventId + " не найдено"));

        // 2. Проверяем принадлежность события пользователю

        if (!event.getInitiatorId().equals(userId)) {
            throw new ForbiddenActionException("Пользователь с ID " + userId + " не является инициатором события " + eventId);
        }

        // 3. Проверяем статус события
        EventState currentState = event.getState();
        if (!currentState.equals(EventState.CANCELED) && !currentState.equals(EventState.PENDING)) {
            throw new ConflictException(
                    "Только отменённые события или события в состоянии ожидания модерации могут быть изменены. Текущий статус: " + currentState
            );
        }

        // 4. Обрабатываем stateAction, если указан
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

        // 5. Применяем обновления полей (только не‑null)
        applyNonNullUpdates(event, updateEventUserRequest);

        // 6. Валидируем дату события
        LocalDateTime updateDate = updateEventUserRequest.getEventDate();
        if (updateDate != null) {
            validateEventDate(updateDate);
        } else if (stateAction == StateAction.SEND_TO_REVIEW) {
            validateEventDate(event.getEventDate());
            event.setRequestModeration(true);
        }

        // 7. Сохраняем и возвращаем результат
        Event updatedEvent = eventRepository.save(event);
        log.info("Событие с ID: {} успешно обновлено", eventId);

        // 8. Получаем рейтинг
        Long rating = getRatingForEvents(List.of(updatedEvent.getId()));

        // 9. Создаём DTO
        EventFullDto dto = toEventFullDto(updatedEvent, rating);

        // 10. ОБОГАЩАЕМ DTO: добавляем initiator и category как полноценные объекты
        Map<Long, UserDto> userMap = getUserMap(List.of(updatedEvent));
        Map<Long, CategoryDto> categoryMap = getCategoryMap(List.of(updatedEvent));

        enrichFullEventDtoWithUserAndCategory(
                dto,
                updatedEvent,
                userMap,
                categoryMap
        );

        return dto;
    }

    @Override
    public List<EventFullDto> getUserEvents(Long userId, int from, int size) {
        log.debug("Начинаем поиск событий для пользователя с ID: {}, from: {}, size: {}", userId, from, size);


        getUserById(userId);
        List<Event> events = eventRepository.findAllByInitiatorIdWithOffset(userId, from, size);

        if (events.isEmpty()) {
            log.debug("Для пользователя с ID {} не найдено событий", userId);
            return Collections.emptyList();
        }

        List<Long> ids = events.stream().map(Event::getId).toList();
        Map<Long, Long> confirmedRequests = getConfirmedRequestsMap(ids);
        Map<Long, Long> ratings = getRatingsMap(ids);

        List<EventFullDto> eventFullDtos = events.stream()
                .peek(event -> event.setConfirmedRequests(confirmedRequests.getOrDefault(event.getId(), 0L)))
                .map(event -> toEventFullDto(event, ratings.getOrDefault(event.getId(), 0L)))
                .collect(Collectors.toList());

        log.info("Найдено {} событий для пользователя с ID {}", events.size(), userId);
        return eventFullDtos;
    }

    @Override
    public EventFullDto getUserEventById(Long userId, Long eventId) {
        log.debug("Начинаем поиск события с ID: {} для пользователя с ID: {}", eventId, userId);

        // Находим пользователя — если не найден, будет выброшено исключение NotFoundException
        getUserById(userId);

        // Ищем событие по ID и проверяем принадлежность пользователю
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (!event.getInitiatorId().equals(userId)) {
            throw new ForbiddenActionException(
                    "Пользователь с ID " + userId + " не является инициатором события " + eventId
            );
        }

        log.debug("Событие найдено в БД: ID {}, заголовок '{}'", event.getId(), event.getTitle());

        // Получаем количество подтверждённых заявок
        List<Long> events = new ArrayList<>();
        events.add(event.getId());
        Long confirmedRequests = getConfirmedRequests(events);
        event.setConfirmedRequests(confirmedRequests);

        // Обновляем просмотры
        setViewsToEvent(event);
        Long rating = getRatingForEvents(List.of(eventId));

        log.info("Полные данные события подготовлены для возврата");
        return toEventFullDto(event, rating);
    }

    // --- Приватные вспомогательные методы ---

    private void validateEventDate(LocalDateTime eventDate) {
        LocalDateTime minEventDate = LocalDateTime.now().plusHours(MIN_HOURS_BEFORE_EVENT);
        if (eventDate.isBefore(minEventDate)) {
            throw new EventCreationRuleException("eventDate", eventDate, "Событие не удовлетворяет правилам создания");
        }
    }

    private CategoryDto getCategoryById(Long categoryId) {
        CategoryDto category = categoryService.getCategoryById(categoryId);

        return category;
    }

    private void setViewsToEvent(Event event) {
        List<ViewStats> stats = getStats(List.of("/events/" + event.getId()));
        event.setViews(stats.stream().findFirst().map(ViewStats::getHits).orElse(0L));
    }

    private List<ViewStats> getStats(List<String> uris) {
        try {
            return statsClient.getStats(LocalDateTime.of(2000, 1, 1, 0, 0, 0), LocalDateTime.now(), uris, true);
        } catch (Exception e) {
            return Collections.emptyList();
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

    private void setViewsToEvents(List<Event> events) {
        if (events.isEmpty()) return;
        List<String> uris = events.stream().map(e -> "/events/" + e.getId()).collect(Collectors.toList());
        List<ViewStats> stats = getStats(uris);
        Map<String, Long> viewsMap = stats.stream().collect(Collectors.toMap(ViewStats::getUri, ViewStats::getHits));
        events.forEach(e -> e.setViews(viewsMap.getOrDefault("/events/" + e.getId(), 0L)));
    }

   /* private Map<Long, Long> getRequestCounts(List<Long> eventIds) {
        return requestRepository.countConfirmedRequestsByEventIds(eventIds, EventState.CONFIRMED).stream()
                .collect(Collectors.toMap(
                        r -> ((Number) r[0]).longValue(),
                        r -> ((Number) r[1]).longValue()
                ));
    }*/


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

        List<Event> eventList = eventRepository.findUserModerationHistory(userId, pageable);
        List<EventFullDto> fullEventDtos = new ArrayList<>();

        if (!eventList.isEmpty()) {
            List<Long> eventIds = eventList.stream()
                    .map(Event::getId)
                    .collect(Collectors.toList());

            // Получаем последние комментарии модерации для событий
            List<ModerationComment> moderationComments = moderationCommentRepository.findLastCommentsByEventIds(eventIds);
            // Создаём маппинг: eventId → ModerationComment
            Map<Long, ModerationComment> commentsMap = moderationComments.stream()
                    .collect(Collectors.toMap(
                            comment -> comment.getEvent().getId(),
                            Function.identity()
                    ));

            fullEventDtos = eventList.stream()
                    .map(event -> {
                        ModerationComment comment = commentsMap.get(event.getId());
                        return EventsMapper.toEventFullDto(event, comment);
                    })
                    .collect(Collectors.toList());
        }

        return fullEventDtos;
    }

    @Override
    public List<EventFullDto> getEventsForModeration(int page, int limit) {
        Pageable pageable = PageRequest.of(page, limit);
        List<Event> events = eventRepository.findByRequestModerationAndState(
                Boolean.TRUE, EventState.PENDING, pageable
        );
        return events.stream()
                .map(EventsMapper::toEventFullDto)
                .collect(Collectors.toList());
    }

    private UserDto getUserById(Long id) {
        UserDto user = userAdminFeign.getById(id);
        if (user == null) {
            throw new NotFoundException("пользователь не найден");
        }
        return user;
    }

    private EventFullDto getEventById(Long id) {
        return findEventById(id);
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

        // Получаем карты зависимостей (только пользователь и категория)
        Map<Long, UserDto> userMap = getUserMap(events);
        Map<Long, CategoryDto> categoryMap = getCategoryMap(events);

        return events.stream()
                .map(event -> {
                    EventFullDto dto = EventsMapper.toEventFullDto(event);
                    enrichFullEventDtoWithUserAndCategory(dto, event, userMap, categoryMap);
                    // Остальные поля (views, rating, confirmedRequests) остаются null/по умолчанию
                    return dto;
                })
                .collect(Collectors.toList());
    }



    public Long getConfirmedRequests(List<Long> ids) {

        List<Object[]> confirmedRequests = requestAdditionalFeign.countRequestsByEventIdsAndStatus(
                ids, EventState.CONFIRMED);
        Long count = confirmedRequests.isEmpty() ? 0L : (Long) confirmedRequests.getFirst()[1];
        return count;
    }

    public Long getRatingForEvents(List<Long> ids) {

        List<Object[]> rating = rateService.getRatingsForEvents(ids);
        Long ratingCount = rating.isEmpty() ? 0L : (Long) rating.getFirst()[1];

        return ratingCount;
    }

    private Map<Long, Long> getViewsMap(List<Long> events) {
        if (events.isEmpty()) return Map.of();

        List<String> uris = events.stream()
                .map(e -> "/events/" + e)
                .collect(toList());

        LocalDateTime start = LocalDateTime.now().minusYears(10);
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

    private Map<Long, UserDto> getUserMap(List<Event> events) {
        List<Long> userIds = events.stream()
                .map(Event::getInitiatorId)
                .distinct()
                .toList();
        if (userIds.isEmpty()) return Map.of();
        return userAdminFeign.getAllInIds(userIds).stream()
                .collect(Collectors.toMap(UserDto::getId, Function.identity()));
    }

    private Map<Long, CategoryDto> getCategoryMap(List<Event> events) {
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

    private void enrichShortEventDtoWithUserAndCategory(EventShortDto dto, Event event,
                                                        Map<Long, UserDto> userMap,
                                                        Map<Long, CategoryDto> categoryMap) {
        UserDto user = userMap.get(event.getInitiatorId());
        if (user != null) {
            dto.setInitiator(new UserShortDto(user.getId(), user.getName()));
        }

        CategoryDto category = categoryMap.get(event.getCategoryId());
        if (category != null) {
            dto.setCategory(category);
        }
    }

    private void enrichFullEventDtoWithUserAndCategory(EventFullDto dto, Event event,
                                                       Map<Long, UserDto> userMap,
                                                       Map<Long, CategoryDto> categoryMap) {
        UserDto user = userMap.get(event.getInitiatorId());
        if (user != null) {
            dto.setInitiator(new UserShortDto(user.getId(), user.getName()));
        }

        CategoryDto category = categoryMap.get(event.getCategoryId());
        if (category != null) {
            dto.setCategory(category);
        }
    }

}
