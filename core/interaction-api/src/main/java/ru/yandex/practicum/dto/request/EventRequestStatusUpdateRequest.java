package ru.yandex.practicum.dto.request;

import lombok.Data;
import ru.yandex.practicum.enums.EventState;

import java.util.List;

@Data
public class EventRequestStatusUpdateRequest {
    private List<Long> requestIds;
    private EventState status;
}

