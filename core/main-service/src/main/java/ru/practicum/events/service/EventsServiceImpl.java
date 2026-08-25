package ru.practicum.events.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import ru.practicum.StatsClient;
import ru.practicum.categories.Category;
import ru.practicum.categories.CategoryRepository;
import ru.practicum.dto.ViewStats;
import ru.practicum.error.exception.ForbiddenActionException;
import ru.practicum.events.*;
import ru.practicum.events.dto.*;
import ru.practicum.error.exception.ConflictException;
import ru.practicum.error.exception.EventCreationRuleException;
import ru.practicum.error.exception.NotFoundException;
import ru.practicum.events.moderation.ModerationComment;
import ru.practicum.events.moderation.ModerationCommentRepository;
import ru.practicum.rating.RateRepository;
import ru.practicum.requests.RequestRepository;
import ru.practicum.user.User;
import ru.practicum.user.UserRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static ru.practicum.events.EventsMapper.*;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class EventsServiceImpl implements EventsService {
    private static final int MIN_HOURS_BEFORE_EVENT = 2;
    private final EventsRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final RequestRepository requestRepository;
    private final UserRepository userRepository;
    private final StatsClient statsClient;
    private final EntityManager entityManager;
    private final RateRepository rateRepository; // <--- ДОБАВЛЕНО
    private final ModerationCommentRepository moderationCommentRepository;

    @Override
    public EventFullDto saveEvent(NewEventDto newEventDto, Long userId) {
        validateEventDate(newEventDto.getEventDate());
        User user = findUserById(userId);
        Category category = findCategoryById(newEventDto.getCategory());

        Event event = toEvent(newEventDto, user, category);
        Event savedEvent = eventRepository.save(event);
        savedEvent.setInitiator(user);
        savedEvent.setCategory(category);

        return toEventFullDto(savedEvent, 0L);
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

        Map<Long, Long> requestCounts = getRequestCounts(events.stream().map(Event::getId).toList());
        Map<Long, Long> ratingsMap = getRatingsMap(events);

        List<EventShortDto> dtoList = events.stream()
                .map(event -> toShortEventDto(event, requestCounts.getOrDefault(event.getId(), 0L), ratingsMap.getOrDefault(event.getId(), 0L)))
                .collect(Collectors.toList());

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

        event.setConfirmedRequests(requestRepository.countByEventIdAndStatus(event.getId(), EventState.CONFIRMED));
        setViewsToEvents(List.of(event));
        Long rating = rateRepository.getRatingForEvent(id);

        return toEventFullDto(event, rating);
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

        if (userIds != null && !userIds.isEmpty()) {
            predicates.add(root.get("initiator").get("id").in(userIds));
        }

        if (states != null && !states.isEmpty()) {
            List<EventState> stateEnums = states.stream()
                    .map(EventState::valueOf)
                    .collect(Collectors.toList());
            predicates.add(root.get("state").in(stateEnums));
        }

        if (categoryIds != null && !categoryIds.isEmpty()) {
            predicates.add(root.get("category").get("id").in(categoryIds));
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

        Map<Long, Long> confirmedRequests = getConfirmedRequestsMap(events);
        Map<Long, Long> ratings = getRatingsMap(events);
        setViewsToEvents(events);

        return events.stream()
                .peek(event -> event.setConfirmedRequests(confirmedRequests.getOrDefault(event.getId(), 0L)))
                .map(event -> toEventFullDto(event, ratings.getOrDefault(event.getId(), 0L)))
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
        saved.setConfirmedRequests(requestRepository.countByEventIdAndStatus(saved.getId(), EventState.CONFIRMED));
        setViewsToEvent(saved);

        Long rating = rateRepository.getRatingForEvent(saved.getId());
        return EventsMapper.toEventFullDto(saved, moderationComment, rating);
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
        User user = event.getInitiator();
        if (!user.getId().equals(userId)) {
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

        Long rating = rateRepository.getRatingForEvent(updatedEvent.getId());
        return toEventFullDto(updatedEvent, rating);
    }

    @Override
    public List<EventFullDto> getUserEvents(Long userId, int from, int size) {
        log.debug("Начинаем поиск событий для пользователя с ID: {}, from: {}, size: {}", userId, from, size);


        User user = findUserById(userId);
        List<Event> events = eventRepository.findAllByInitiatorIdWithOffset(user.getId(), from, size);

        if (events.isEmpty()) {
            log.debug("Для пользователя с ID {} не найдено событий", userId);
            return Collections.emptyList();
        }

        Map<Long, Long> confirmedRequests = getConfirmedRequestsMap(events);
        Map<Long, Long> ratings = getRatingsMap(events);

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
        User user = findUserById(userId);

        // Ищем событие по ID и проверяем принадлежность пользователю
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (!event.getInitiator().getId().equals(userId)) {
            throw new ForbiddenActionException(
                    "Пользователь с ID " + userId + " не является инициатором события " + eventId
            );
        }

        log.debug("Событие найдено в БД: ID {}, заголовок '{}'", event.getId(), event.getTitle());

        // Получаем количество подтверждённых заявок
        long confirmedRequests = requestRepository.countByEventIdAndStatus(event.getId(), EventState.CONFIRMED);
        event.setConfirmedRequests(confirmedRequests);

        // Обновляем просмотры
        setViewsToEvent(event);
        Long rating = rateRepository.getRatingForEvent(eventId);

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

    private Category findCategoryById(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EventCreationRuleException("categoryId", categoryId,
                        "Категория с ID " + categoryId + " не найдена в базе данных"));
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с ID " + userId + " не найден"));
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

    private Map<Long, Long> getConfirmedRequestsMap(List<Event> events) {
        if (events.isEmpty()) return Map.of();
        List<Long> eventIds = events.stream().map(Event::getId).collect(Collectors.toList());
        return requestRepository.countConfirmedRequestsByEventIds(eventIds, EventState.CONFIRMED).stream()
                .collect(Collectors.toMap(r -> ((Number) r[0]).longValue(), r -> ((Number) r[1]).longValue()));
    }

    private void setViewsToEvents(List<Event> events) {
        if (events.isEmpty()) return;
        List<String> uris = events.stream().map(e -> "/events/" + e.getId()).collect(Collectors.toList());
        List<ViewStats> stats = getStats(uris);
        Map<String, Long> viewsMap = stats.stream().collect(Collectors.toMap(ViewStats::getUri, ViewStats::getHits));
        events.forEach(e -> e.setViews(viewsMap.getOrDefault("/events/" + e.getId(), 0L)));
    }

    private Map<Long, Long> getRequestCounts(List<Long> eventIds) {
        return requestRepository.countConfirmedRequestsByEventIds(eventIds, EventState.CONFIRMED).stream()
                .collect(Collectors.toMap(
                        r -> ((Number) r[0]).longValue(),
                        r -> ((Number) r[1]).longValue()
                ));
    }

    private Map<Long, Long> getRatingsMap(List<Event> events) {
        if (events.isEmpty()) return Map.of();
        List<Long> eventIds = events.stream().map(Event::getId).collect(Collectors.toList());
        List<Object[]> results = rateRepository.getRatingsForEvents(eventIds);
        return results.stream().collect(Collectors.toMap(
                row -> ((Number) row[0]).longValue(),
                row -> ((Number) row[1]).longValue()
        ));
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
            Category category = categoryRepository.findById(request.getCategory())
                    .orElseThrow(() -> new NotFoundException("Category with id=" + request.getCategory() + " was not found"));
            event.setCategory(category);
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
}
