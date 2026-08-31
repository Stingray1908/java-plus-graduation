package ru.yandex.practicum.feigns.request;

import jakarta.validation.constraints.Positive;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.dto.request.ParticipationRequestDto;

import java.util.List;

@FeignClient(name = "request-service", path = "/users/{userId}/requests")
public interface PrivateParticipationRequestFeign {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipationRequestDto createParticipationRequest(
            @PathVariable @Positive Long userId,
            @RequestParam @Positive Long eventId);

    @PatchMapping("/{requestId}/cancel")
    @ResponseStatus(HttpStatus.OK)
    public ParticipationRequestDto cancelParticipationRequest(
            @PathVariable Long userId,
            @PathVariable Long requestId);

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<ParticipationRequestDto> getUserParticipationRequests(
            @PathVariable @Positive Long userId);
}
