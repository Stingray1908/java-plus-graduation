package ru.practicum.comments;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateCommentByAuthorRequest {
    @Size(max = 1000, message = "Comment text cannot exceed 1000 characters")
    @NotBlank(message = "Comment text must not be blank")
    private String text;
}
