package ru.yandex.practicum.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;
import ru.yandex.practicum.enums.EventState;
import ru.yandex.practicum.feigns.request.RequestAdditionalFeign;
import ru.yandex.practicum.service.RequestsService;

import java.util.List;
@Service
@RequiredArgsConstructor
public class RequestAdditionalController implements RequestAdditionalFeign {

    private final RequestsService requestsService;

    @Override
    public List<Object[]> countRequestsByEventIdsAndStatus(List<Long> eventIds, EventState status) {

    }
}
