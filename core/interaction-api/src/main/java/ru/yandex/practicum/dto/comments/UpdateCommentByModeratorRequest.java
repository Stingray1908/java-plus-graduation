package ru.yandex.practicum.dto.comments;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import ru.yandex.practicum.enums.CommentStatus;

@Data
public class UpdateCommentByModeratorRequest {
    @Size(max = 1000, message = "Comment text cannot exceed 1000 characters")
    private String text;

    @NotNull(message = "Status must not be null for moderator")
    private CommentStatus status;
}
