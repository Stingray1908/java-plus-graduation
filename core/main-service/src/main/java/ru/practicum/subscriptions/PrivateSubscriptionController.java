package ru.practicum.subscriptions;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.events.dto.EventShortDto;

import java.util.List;

@RestController
@RequestMapping("/users/{userId}/subscriptions")
@RequiredArgsConstructor
@Slf4j
@Validated
public class PrivateSubscriptionController {
    private final SubscriptionService subscriptionService;

    @PostMapping("/{publisherId}")
    @ResponseStatus(HttpStatus.CREATED)
    public void subscribe(
            @PathVariable @Positive Long userId,
            @PathVariable @Positive Long publisherId) {
        log.info("Получен запрос на подписку пользователя {} на пользователя {}", userId, publisherId);
        subscriptionService.subscribe(userId, publisherId);
    }

    @DeleteMapping("/{publisherId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unsubscribe(
            @PathVariable @Positive Long userId,
            @PathVariable @Positive Long publisherId) {
        log.info("Получен запрос на отписку пользователя {} от пользователя {}", userId, publisherId);
        subscriptionService.unsubscribe(userId, publisherId);
    }

    @GetMapping("/events")
    @ResponseStatus(HttpStatus.OK)
    public List<EventShortDto> getActualEventsFromSubscriptions(
            @PathVariable @Positive Long userId,
            @RequestParam(defaultValue = "0") @Min(0) int from,
            @RequestParam(defaultValue = "10") @Positive int size) {
        log.info("Получен запрос на актуальные события подписок пользователя {}", userId);
        return subscriptionService.getActualEventsFromSubscriptions(userId, from, size);
    }
}
