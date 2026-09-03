package ru.yandex.practicum.rating.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.rating.service.RateService;
import ru.yandex.practicum.feigns.main.rate.RatePrivateFeign;

@RestController
@RequestMapping("/users/{userId}/events/{eventId}/rate")
@RequiredArgsConstructor
public class RatePrivateController {

    private final RateService rateService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void addRate(
            @PathVariable Long userId,
            @PathVariable Long eventId,
            @RequestParam Boolean isLike) {
        rateService.addRate(userId, eventId, isLike);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRate(
            @PathVariable Long userId,
            @PathVariable Long eventId) {
        rateService.deleteRate(userId, eventId);
    }
}
