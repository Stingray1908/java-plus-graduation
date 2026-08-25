package ru.practicum.rating.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.rating.service.RateService;

@RestController
@RequestMapping("/users/{userId}/events/{eventId}/rate")
@RequiredArgsConstructor
public class PrivateRateController {

    private final RateService rateService;

    // POST /users/{userId}/events/{eventId}/rate?isLike=true (или false)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void addRate(
            @PathVariable Long userId,
            @PathVariable Long eventId,
            @RequestParam Boolean isLike) {
        rateService.addRate(userId, eventId, isLike);
    }

    // DELETE /users/{userId}/events/{eventId}/rate
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRate(
            @PathVariable Long userId,
            @PathVariable Long eventId) {
        rateService.deleteRate(userId, eventId);
    }
}