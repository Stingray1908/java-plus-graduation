package ru.yandex.practicum.service;

import ru.yandex.practicum.dto.request.EventRequestStatusUpdateRequest;
import ru.yandex.practicum.dto.request.EventRequestStatusUpdateResult;
import ru.yandex.practicum.dto.request.ParticipationRequestDto;

import java.util.List;

public interface RequestsService {
    EventRequestStatusUpdateResult updateRequestStatuses(
            Long userId, Long eventId, EventRequestStatusUpdateRequest request);

    List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId);
}
