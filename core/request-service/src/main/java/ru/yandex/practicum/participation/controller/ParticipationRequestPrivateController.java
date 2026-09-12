package ru.yandex.practicum.participation.controller;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.CollectorClient;
import ru.yandex.practicum.dto.events.EventFullDto;
import ru.yandex.practicum.dto.request.ParticipationRequestDto;
import ru.yandex.practicum.participation.service.ParticipationsRequestsService;
import stats.service.collector.UserActionProtoOuterClass;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/users/{userId}/requests")
@RequiredArgsConstructor
@Slf4j
public class ParticipationRequestPrivateController {

    private final CollectorClient collectorClient;
    private final ParticipationsRequestsService participationsRequestsService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipationRequestDto createParticipationRequest(
            @PathVariable @Positive Long userId,
            @RequestParam @Positive Long eventId) {
        log.info("Получен запрос на создание заявки пользователя {} на участие в событии {}", userId, eventId);

        ParticipationRequestDto createdRequest = participationsRequestsService.createParticipationRequest(userId, eventId);

        log.info("Заявка успешно создана с ID: {} для пользователя {} на событие {}", createdRequest.getId(), userId, eventId);
        sendUserAction(userId, eventId, UserActionProtoOuterClass.ActionTypeProto.ACTION_REGISTER);
        return createdRequest;
    }

    @PatchMapping("/{requestId}/cancel")
    @ResponseStatus(HttpStatus.OK)
    public ParticipationRequestDto cancelParticipationRequest(
            @PathVariable Long userId,
            @PathVariable Long requestId) {

        ParticipationRequestDto cancelledRequest = participationsRequestsService.cancelParticipationRequest(userId, requestId);
        return cancelledRequest;
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<ParticipationRequestDto> getUserParticipationRequests(
            @PathVariable @Positive Long userId) {
        log.info("Получен запрос на получение заявок пользователя с ID: {}", userId);

        List<ParticipationRequestDto> requests = participationsRequestsService.getUserParticipationRequests(userId);

        log.info("Возвращено {} заявок для пользователя с ID: {}", requests.size(), userId);
        return requests;
    }

private void sendUserAction (long userId, long eventId, UserActionProtoOuterClass.ActionTypeProto actionType){
    collectorClient.sendUserAction(
            userId,
            eventId,
            actionType,
            Instant.now());
}

}

