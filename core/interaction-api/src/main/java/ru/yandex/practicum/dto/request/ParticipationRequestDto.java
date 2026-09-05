package ru.yandex.practicum.dto.request;

import lombok.Builder;
import lombok.Data;
import ru.yandex.practicum.enums.EventState;

@Builder
@Data
public class ParticipationRequestDto {
    private String created;
    private Long event;
    private Long id;
    private Long requester;
    private EventState status;
}
