package ru.yandex.practicum.feigns.request;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.enums.EventState;

import java.util.List;

@FeignClient(name = "request-service", contextId = "requestAdditionalFeign", path = "users/requests")
public interface RequestAdditionalFeign {

    @GetMapping
    List<Object[]> countRequestsByEventIdsAndStatus(
            @RequestParam("eventIds") List<Long> eventIds,
            @RequestParam("status") EventState status);
}
