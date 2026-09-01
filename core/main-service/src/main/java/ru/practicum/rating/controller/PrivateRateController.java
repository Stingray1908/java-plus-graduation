package ru.practicum.rating.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.rating.service.RateService;
import ru.yandex.practicum.feigns.main.rate.PrivateRateFeign;

@RestController
@RequestMapping("/users/{userId}/events/{eventId}/rate")
@RequiredArgsConstructor
public class PrivateRateController implements PrivateRateFeign {

    private final RateService rateService;

    @Override
    public void addRate(
            @PathVariable Long userId,
            @PathVariable Long eventId,
            @RequestParam Boolean isLike) {
        rateService.addRate(userId, eventId, isLike);
    }

    @Override
    public void deleteRate(
            @PathVariable Long userId,
            @PathVariable Long eventId) {
        rateService.deleteRate(userId, eventId);
    }
}
