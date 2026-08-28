package ru.practicum.subscriptions;

import ru.practicum.events.dto.EventShortDto;

import java.util.List;

public interface SubscriptionService {

    void subscribe(Long userId, Long publisherId);

    void unsubscribe(Long userId, Long publisherId);

    List<EventShortDto> getActualEventsFromSubscriptions(Long userId, int from, int size);
}
