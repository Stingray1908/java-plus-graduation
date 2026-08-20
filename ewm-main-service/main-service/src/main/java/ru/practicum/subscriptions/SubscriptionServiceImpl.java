package ru.practicum.subscriptions;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.error.exception.ConflictException;
import ru.practicum.error.exception.NotFoundException;
import ru.practicum.events.Event;
import ru.practicum.events.EventState;
import ru.practicum.events.EventsMapper;
import ru.practicum.events.EventsRepository;
import ru.practicum.events.dto.EventShortDto;
import ru.practicum.requests.RequestRepository;
import ru.practicum.user.User;
import ru.practicum.user.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SubscriptionServiceImpl implements SubscriptionService {
    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final EventsRepository eventsRepository;
    private final RequestRepository requestRepository;

    @Override
    public void subscribe(Long userId, Long publisherId) {
        log.info("Пользователь с ID {} подписывается на пользователя с ID {}", userId, publisherId);

        if (userId.equals(publisherId)) {
            throw new ConflictException("User cannot subscribe to himself");
        }

        User subscriber = findUserById(userId);
        User publisher = findUserById(publisherId);

        if (subscriptionRepository.existsBySubscriber_IdAndPublisher_Id(userId, publisherId)) {
            throw new ConflictException("User with id=" + userId + " is already subscribed to user with id=" + publisherId);
        }

        subscriptionRepository.save(new Subscription(null, subscriber, publisher, LocalDateTime.now()));
        log.info("Пользователь с ID {} успешно подписался на пользователя с ID {}", userId, publisherId);
    }

    @Override
    public void unsubscribe(Long userId, Long publisherId) {
        log.info("Пользователь с ID {} отписывается от пользователя с ID {}", userId, publisherId);

        findUserById(userId);
        findUserById(publisherId);

        int deleted = subscriptionRepository.deleteBySubscriberIdAndPublisherId(userId, publisherId);
        if (deleted == 0) {
            throw new NotFoundException("Subscription from user with id=" + userId +
                    " to user with id=" + publisherId + " was not found");
        }

        log.info("Пользователь с ID {} успешно отписался от пользователя с ID {}", userId, publisherId);
    }

    @Transactional(readOnly = true)
    @Override
    public List<EventShortDto> getActualEventsFromSubscriptions(Long userId, int from, int size) {
        log.info("Получение актуальных событий пользователя с ID {}, from: {}, size: {}", userId, from, size);

        findUserById(userId);

        List<Event> events = eventsRepository.findActualPublishedEventsBySubscriberId(
                userId,
                EventState.PUBLISHED,
                LocalDateTime.now(),
                PageRequest.of(from / size, size)
        );

        Map<Long, Long> confirmedRequests = getConfirmedRequests(events);

        return events.stream()
                .map(event -> EventsMapper.toShortEventDto(event, confirmedRequests.getOrDefault(event.getId(), 0L)))
                .collect(Collectors.toList());
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с ID " + userId + " не найден"));
    }

    private Map<Long, Long> getConfirmedRequests(List<Event> events) {
        if (events.isEmpty()) {
            return Map.of();
        }

        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .collect(Collectors.toList());

        return requestRepository.countConfirmedRequestsByEventIds(eventIds, EventState.CONFIRMED).stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> ((Number) row[1]).longValue()
                ));
    }
}
