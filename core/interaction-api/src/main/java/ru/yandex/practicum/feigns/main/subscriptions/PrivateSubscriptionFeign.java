package ru.yandex.practicum.feigns.main.subscriptions;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.dto.events.EventFullDto;
import ru.yandex.practicum.dto.events.EventShortDto;
import ru.yandex.practicum.enums.EventState;

import java.time.LocalDateTime;
import java.util.List;

@FeignClient(name = "main-service", contextId = "privateSubscriptionFeign", path = "/users/{userId}/subscriptions")
@Validated
public interface PrivateSubscriptionFeign {

    @PostMapping("/{publisherId}")
    @ResponseStatus(HttpStatus.CREATED)
    public void subscribe(
            @PathVariable @Positive Long userId,
            @PathVariable @Positive Long publisherId);

    @DeleteMapping("/{publisherId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unsubscribe(
            @PathVariable @Positive Long userId,
            @PathVariable @Positive Long publisherId);

    @GetMapping("/events")
    @ResponseStatus(HttpStatus.OK)
    public List<EventShortDto> getActualEventsFromSubscriptions(
            @PathVariable @Positive Long userId,
            @RequestParam(defaultValue = "0") @Min(0) int from,
            @RequestParam(defaultValue = "10") @Positive int size);
}
