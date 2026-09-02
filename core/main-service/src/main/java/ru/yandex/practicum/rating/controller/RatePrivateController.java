package ru.yandex.practicum.rating.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.rating.service.RateService;
import ru.yandex.practicum.feigns.main.rate.RatePrivateFeign;

@RestController
@RequestMapping("/users/{userId}/events/{eventId}/rate")
@RequiredArgsConstructor
public class RatePrivateController implements RatePrivateFeign {

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
