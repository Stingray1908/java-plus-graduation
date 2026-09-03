package ru.yandex.practicum.event.moderation;

import ru.yandex.practicum.dto.events.moderation.ModerationCommentShortDto;

import static ru.yandex.practicum.Constance.FORMATTER;

public class ModerationMapper {
    public static ModerationCommentShortDto moderationCommentShortDto(ModerationComment mc) {
        ModerationCommentShortDto dto = new ModerationCommentShortDto();
        dto.setId(mc.getId());
        dto.setCommentText(mc.getCommentText());
        dto.setCreatedOn(mc.getCreatedOn().format(FORMATTER));
        return dto;
    }
}
