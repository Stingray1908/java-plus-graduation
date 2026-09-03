package ru.yandex.practicum.feigns.event;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.yandex.practicum.dto.events.EventFullDto;
import ru.yandex.practicum.dto.events.EventShortDto;

import java.time.LocalDateTime;
import java.util.List;

@FeignClient(name = "event-service", contextId = "eventsPublicFeign", path = "/events")
public interface EventsPublicFeign {

    @GetMapping("/{id}")
    public ResponseEntity<EventFullDto> getEventById(
            @PathVariable Long id,
            HttpServletRequest request
    );
}
