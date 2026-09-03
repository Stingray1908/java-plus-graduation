package ru.yandex.practicum.subscriptions;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.dto.events.EventFullDto;
import ru.yandex.practicum.dto.events.EventShortDto;
import ru.yandex.practicum.dto.user.UserDto;
import ru.yandex.practicum.enums.EventState;
import ru.yandex.practicum.error.exception.ConflictException;
import ru.yandex.practicum.error.exception.NotFoundException;
import ru.yandex.practicum.event.service.EventsService;
import ru.yandex.practicum.feigns.request.RequestAdditionalFeign;
import ru.yandex.practicum.feigns.user.UserAdminFeign;

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
    private final UserAdminFeign userAdminFeign;
    private final RequestAdditionalFeign requestAdditionalFeign;
    private final EventsService eventsService;


    @Override
    public void subscribe(Long userId, Long publisherId) {
        log.info("Пользователь с ID {} подписывается на пользователя с ID {}", userId, publisherId);

        if (userId.equals(publisherId)) {
            throw new ConflictException("User cannot subscribe to himself");
        }

        UserDto subscriber = getUserById(userId);
        UserDto publisher = getUserById(publisherId);

        if (subscriptionRepository.existsBySubscriber_IdAndPublisher_Id(userId, publisherId)) {
            throw new ConflictException("User with id=" + userId + " is already subscribed to user with id=" + publisherId);
        }

        subscriptionRepository.save(new Subscription(null, userId, publisherId, LocalDateTime.now()));
        log.info("Пользователь с ID {} успешно подписался на пользователя с ID {}", userId, publisherId);
    }

    @Override
    public void unsubscribe(Long userId, Long publisherId) {
        log.info("Пользователь с ID {} отписывается от пользователя с ID {}", userId, publisherId);

        getUserById(userId);
        getUserById(publisherId);

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

        getUserById(userId);
        //вызывался из репо
        // пишем запрос в Феигн
        // Эвент феигн
        // эвент сервис
        // эвент репо

        List<EventFullDto> events = eventsService.findActualPublishedEventsBySubscriberId(
                userId,
                EventState.PUBLISHED,
                LocalDateTime.now(),
                //PageRequest.of(from / size, size)
                from,
                size
        );

        List<Long> ids = events.stream().map(EventFullDto::getId).toList();

        //Map<Long, Long> confirmedRequests = getConfirmedRequests(ids);

        return eventsService.getEventShortDtoByIdsWithStats(ids);
        /* events.stream()
                .map(event -> EventsMapper.toShortEventDto(event, confirmedRequests.getOrDefault(event.getId(), 0L)))
                .collect(Collectors.toList());*/
    }

    @Override
    public List<Long> findPublishersBySubscriber(Long subscriberId) {
        return subscriptionRepository.findPublishersBySubscriber(subscriberId);
    }

    private Map<Long, Long> getConfirmedRequests(List<Long> eventIds) {
        if (eventIds.isEmpty()) {
            return Map.of();
        }

        return requestAdditionalFeign.countRequestsByEventIdsAndStatus(eventIds, EventState.CONFIRMED).stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> ((Number) row[1]).longValue()
                ));
    }

    private UserDto getUserById(Long id) {
        UserDto user = userAdminFeign.getById(id);
        if (user == null) throw new NotFoundException("Пользователь с ID " + id + " не найден");
        return user;
    }

    private EventFullDto getEventById(Long id) {
        return eventsService.findEventById(id);
    }
}
