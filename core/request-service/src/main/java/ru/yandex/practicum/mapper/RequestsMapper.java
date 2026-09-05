package ru.yandex.practicum.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.dto.request.ParticipationRequestDto;
import ru.yandex.practicum.entity.ParticipationRequest;

import java.util.List;
import java.util.stream.Collectors;

import static ru.yandex.practicum.Constants.FORMATTER;

@Component
public class RequestsMapper {

    public static ParticipationRequestDto toDto(ParticipationRequest request) {
        return ParticipationRequestDto.builder()
                .created(request.getCreated().format(FORMATTER))
                .event(request.getEventId())
                .id(request.getId())
                .requester(request.getRequesterId())
                .status(request.getStatus())
                .build();
    }

    public static List<ParticipationRequestDto> toDtoList(List<ParticipationRequest> requests) {
        return requests.stream()
                .map(RequestsMapper::toDto)
                .collect(Collectors.toList());
    }
}
