package ru.practicum.comments;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class NewCommentDto {

    @NotBlank(message = "Comment text cannot be blank")
    @NotNull(message = "Comment cannot be null")
    @Size(max = 1000, message = "Comment text cannot exceed 1000 characters")
    private String text;
}
