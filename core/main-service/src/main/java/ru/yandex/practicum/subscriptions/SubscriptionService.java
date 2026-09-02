package ru.yandex.practicum.subscriptions;

import org.springframework.data.repository.query.Param;
import ru.yandex.practicum.dto.events.EventShortDto;

import java.util.List;

public interface SubscriptionService {

    void subscribe(Long userId, Long publisherId);

    void unsubscribe(Long userId, Long publisherId);

    List<EventShortDto> getActualEventsFromSubscriptions(Long userId, int from, int size);

    List<Long> findPublishersBySubscriber(@Param("subscriberId") Long subscriberId);
}
