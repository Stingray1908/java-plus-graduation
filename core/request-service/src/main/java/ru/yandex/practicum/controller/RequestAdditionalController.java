package ru.yandex.practicum.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.enums.EventState;
import ru.yandex.practicum.feigns.request.RequestAdditionalFeign;
import ru.yandex.practicum.service.RequestsServiceImpl;

import java.util.List;

@RestController
@RequestMapping("/users/requests")
@RequiredArgsConstructor

public class RequestAdditionalController implements RequestAdditionalFeign {

    private final RequestsServiceImpl requestsService;


    @Override
    public List<Object[]> countRequestsByEventIdsAndStatus(List<Long> eventIds, EventState status) {
        return requestsService.countRequestsByEventIdsAndStatus(eventIds, status);
    }
}
