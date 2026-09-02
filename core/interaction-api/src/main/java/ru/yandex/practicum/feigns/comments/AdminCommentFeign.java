package ru.yandex.practicum.feigns.comments;

import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.dto.comments.CommentDto;
import ru.yandex.practicum.dto.comments.UpdateCommentByModeratorRequest;

import java.util.List;

@FeignClient(name = "comment-service", contextId = "adminCommentFeign", path = "/admin/comments")
public interface AdminCommentFeign {

    @GetMapping
    List<CommentDto> getAllComments(
            @RequestParam(required = false) Long eventId,
            @RequestParam(defaultValue = "0") Integer from,
            @RequestParam(defaultValue = "10") Integer size);

    @PatchMapping("/{commentId}")
    CommentDto moderateComment(
            @PathVariable Long commentId,
            @Valid @RequestBody UpdateCommentByModeratorRequest request);

    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteCommentByAdmin(@PathVariable Long commentId);
}
