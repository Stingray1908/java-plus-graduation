package ru.practicum.events.moderation;

import ru.practicum.events.dto.moderation.ModerationCommentShortDto;

import static ru.practicum.common.Constance.FORMATTER;

public class ModerationMapper {
    public static ModerationCommentShortDto moderationCommentShortDto(ModerationComment mc) {
        ModerationCommentShortDto dto = new ModerationCommentShortDto();
        dto.setId(mc.getId());
        dto.setCommentText(mc.getCommentText());
        dto.setCreatedOn(mc.getCreatedOn().format(FORMATTER));
        return dto;
    }
}
