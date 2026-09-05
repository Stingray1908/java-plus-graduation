package ru.yandex.practicum.event.moderation;

import ru.yandex.practicum.dto.events.moderation.ModerationCommentShortDto;

import java.util.List;

import static ru.yandex.practicum.Constants.FORMATTER;

public class ModerationMapper {
    public static ModerationCommentShortDto toModerationCommentShortDto(ModerationComment mc) {
        ModerationCommentShortDto dto = new ModerationCommentShortDto();
        dto.setId(mc.getId());
        dto.setCommentText(mc.getCommentText());
        dto.setCreatedOn(mc.getCreatedOn().format(FORMATTER));
        return dto;
    }

    public static List<ModerationCommentShortDto> toListModerationCommentShortDto(List<ModerationComment> mcs) {
        return mcs.stream()
                .map(ModerationMapper::toModerationCommentShortDto)
                .toList();
    }


}
