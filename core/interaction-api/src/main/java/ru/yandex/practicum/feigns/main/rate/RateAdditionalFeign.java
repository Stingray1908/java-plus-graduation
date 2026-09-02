package ru.yandex.practicum.feigns.main.rate;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@FeignClient(name = "main-service", path = "/users/{userId}/events/{eventId}/rate")

public interface RateAdditionalFeign {

    @GetMapping
    List<Object[]> getRatingsForEvents(@RequestParam List<Long> eventIds);
}
