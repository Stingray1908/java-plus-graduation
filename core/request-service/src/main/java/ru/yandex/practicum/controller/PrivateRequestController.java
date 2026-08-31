package ru.yandex.practicum.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.dto.request.EventRequestStatusUpdateRequest;
import ru.yandex.practicum.dto.request.EventRequestStatusUpdateResult;
import ru.yandex.practicum.dto.request.ParticipationRequestDto;
import ru.yandex.practicum.feigns.request.PrivateRequestFeign;
import ru.yandex.practicum.service.RequestsService;

import java.util.List;

@RestController
@RequestMapping("/users/{userId}/events/{eventId}/requests")
@RequiredArgsConstructor
@Slf4j
public class PrivateRequestController implements PrivateRequestFeign {

    private final RequestsService requestsService;

    @Override
    public EventRequestStatusUpdateResult updateRequestStatus(
            @PathVariable Long userId,
            @PathVariable Long eventId,
            @RequestBody EventRequestStatusUpdateRequest request) {

        log.info("Получен запрос на изменение статуса заявок для пользователя {} события {}", userId, eventId);
        log.debug("Данные запроса: {}", request);

        EventRequestStatusUpdateResult result = requestsService.updateRequestStatuses(userId, eventId, request);

        log.info("Статус заявок успешно изменён для пользователя {} события {}", userId, eventId);
        log.debug("Результат операции: {}", result);

        return result;
    }

    @Override
    public List<ParticipationRequestDto> getEventRequests(
            @PathVariable Long userId,
            @PathVariable Long eventId) {

        log.info("Получен запрос на получение заявок для пользователя {} события {}", userId, eventId);

        List<ParticipationRequestDto> requests = requestsService.getEventRequests(userId, eventId);

        log.info("Найдено {} заявок для пользователя {} события {}", requests.size(), userId, eventId);
        return requests;
    }
}
