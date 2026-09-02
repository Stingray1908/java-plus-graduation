package ru.practicum.rating.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.rating.service.RateService;
import ru.practicum.rating.service.RateServiceImpl;
import ru.yandex.practicum.feigns.main.rate.RateAdditionalFeign;

import java.util.List;
@RestController
@RequestMapping("/users/{userId}/events/{eventId}/rate")
@RequiredArgsConstructor
public class RateAdditionalController implements RateAdditionalFeign {

    private final RateServiceImpl rateService;

    @Override
    public List<Object[]> getRatingsForEvents(List<Long> eventIds) {
        return rateService.getRatingsForEvents(eventIds);
    }
}
