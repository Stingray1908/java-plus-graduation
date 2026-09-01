package ru.yandex.practicum.feigns.main.rate;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;


@FeignClient(name = "main-service", path = "/users/{userId}/events/{eventId}/rate")

public interface PrivateRateFeign {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void addRate(
            @PathVariable Long userId,
            @PathVariable Long eventId,
            @RequestParam Boolean isLike);

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRate(
            @PathVariable Long userId,
            @PathVariable Long eventId);
}
