package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.dto.events.EventFullDto;
import ru.yandex.practicum.entity.Event;
import ru.yandex.practicum.error.exception.NotFoundException;
import ru.yandex.practicum.repo.EventsRepository;

import static ru.yandex.practicum.mapper.EventsMapper.toEventFullDto;

@RequiredArgsConstructor
@Service
public class EventAdditionalService {

    private final EventsRepository eventsRepository;

    public EventFullDto findEventById(Long id) {
        Event event = eventsRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Comment not found"));
        EventFullDto dto = toEventFullDto(event);
        return dto;
    }
}
