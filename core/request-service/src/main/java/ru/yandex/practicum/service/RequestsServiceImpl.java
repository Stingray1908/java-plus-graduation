package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.coyote.BadRequestException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.dto.events.EventFullDto;
import ru.yandex.practicum.dto.request.EventRequestStatusUpdateRequest;
import ru.yandex.practicum.dto.request.EventRequestStatusUpdateResult;
import ru.yandex.practicum.dto.request.ParticipationRequestDto;
import ru.yandex.practicum.entity.ParticipationRequest;
import ru.yandex.practicum.enums.EventState;
import ru.yandex.practicum.error.exception.ConflictException;
import ru.yandex.practicum.error.exception.ForbiddenActionException;
import ru.yandex.practicum.error.exception.NotFoundException;
import ru.yandex.practicum.feigns.event.EventsPublicFeign;
import ru.yandex.practicum.mapper.RequestsMapper;
import ru.yandex.practicum.repo.RequestRepository;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class RequestsServiceImpl implements RequestsService {

    private final RequestRepository requestRepository;
    private final EventsPublicFeign eventsAdminFeign;

    @Override
    public EventRequestStatusUpdateResult updateRequestStatuses(
            Long userId, Long eventId, EventRequestStatusUpdateRequest request) {

        // 1. Проверяем существование события и принадлежность пользователю
        EventFullDto event = getEventById(eventId);

        if (!event.getInitiator().getId().equals(userId)) {
            throw new ForbiddenActionException("User is not the initiator of the event");
        }

        // 2. Проверяем условия пре‑модерации и лимита (400 BAD_REQUEST)
        if (event.getParticipantLimit() == 0 || !event.getRequestModeration()) {
            throw new ForbiddenActionException("Request moderation is not required for this event");
        }

        // 3. Находим заявки для обновления
        List<ParticipationRequest> requests = requestRepository.findAllById(request.getRequestIds());
        if (requests.isEmpty()) {
            throw new NotFoundException("No requests found for the given IDs");
        }

        // 4. Проверяем, что все заявки в статусе PENDING (409 CONFLICT)
        boolean allPending = requests.stream()
                .allMatch(r -> r.getStatus() == EventState.PENDING);
        if (!allPending) {
            throw new ConflictException("All requests must be in PENDING status");
        }

        // 5. Проверяем лимит участников с учётом новых подтверждений
        long confirmedCount = requestRepository.countByEventIdAndStatus(eventId, EventState.CONFIRMED);
        int newConfirmedCount = (int) confirmedCount + request.getRequestIds().size();

        List<ParticipationRequest> confirmed = new ArrayList<>();
        List<ParticipationRequest> rejected = new ArrayList<>();

        if (newConfirmedCount > event.getParticipantLimit()) {
            // 6. Автоматическое отклонение всех неподтверждённых заявок при исчерпании лимита
            List<ParticipationRequest> allPendingRequests = requestRepository
                    .findByEventIdAndStatus(eventId, EventState.PENDING);

            for (ParticipationRequest req : allPendingRequests) {
                req.setStatus(EventState.REJECTED);
                rejected.add(req);
            }
            requestRepository.saveAll(allPendingRequests);

            throw new ConflictException("The participant limit has been reached. All pending requests have been rejected.");
        } else {
            // 7. Обычное обновление статусов
            for (ParticipationRequest req : requests) {
                if (request.getStatus() == EventState.CONFIRMED) {
                    req.setStatus(EventState.CONFIRMED);
                    confirmed.add(req);
                } else if (request.getStatus() == EventState.REJECTED) {
                    req.setStatus(EventState.REJECTED);
                    rejected.add(req);
                }
            }
            requestRepository.saveAll(requests);
        }

        return EventRequestStatusUpdateResult.builder()
                .confirmedRequests(RequestsMapper.toDtoList(confirmed))
                .rejectedRequests(RequestsMapper.toDtoList(rejected))
                .build();
    }

    @Override
    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        // 1. Проверяем существование события и принадлежность пользователю
        EventFullDto event = getEventById(eventId);

        if (!event.getInitiator().getId().equals(userId)) {
            throw new ForbiddenActionException("User is not the initiator of the event");
        }

        // 2. Получаем все заявки на событие
        List<ParticipationRequest> requests = requestRepository.findByEventId(eventId);

        // 3. Преобразуем в DTO
        return RequestsMapper.toDtoList(requests);
    }

    private EventFullDto getEventById(Long id) {
        ResponseEntity<EventFullDto> event = eventsAdminFeign.getEventByIdInside(id);
        if (!event.getStatusCode().is2xxSuccessful()) {
            throw new NotFoundException("событие не найдено");
        }
        return event.getBody();
    }

    public List<Object[]> countRequestsByEventIdsAndStatus(List<Long> eventIds, EventState status) {
        return requestRepository.countRequestsByEventIdsAndStatus(eventIds, status);
    }
}
