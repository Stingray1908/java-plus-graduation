package ru.practicum.events.dto.moderation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModerationCommentShortDto {
    private Long id;
    private String commentText;
    private String createdOn;
}

