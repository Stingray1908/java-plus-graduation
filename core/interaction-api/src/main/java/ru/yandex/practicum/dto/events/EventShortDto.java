package ru.yandex.practicum.dto.events;

import lombok.Data;
import ru.yandex.practicum.dto.categories.CategoryDto;
import ru.yandex.practicum.dto.user.UserShortDto;

@Data
public class EventShortDto {
    private Long id;
    private String annotation;
    private CategoryDto category;
    private Long confirmedRequests;
    private String eventDate;
    private UserShortDto initiator;
    private Boolean paid;
    private String title;
    private Long views;
    private Long rating;
}
