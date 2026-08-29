package ru.yandex.practicum.feigns.comments;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.dto.comments.CommentDto;
import ru.yandex.practicum.dto.comments.NewCommentDto;
import ru.yandex.practicum.dto.comments.UpdateCommentByAuthorRequest;

import java.util.List;
@FeignClient(name = "comment-service", path = "/users/{userId}/events/{eventId}/comments")
public interface PrivateCommentFeign {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    CommentDto addComment(
            @PathVariable Long userId,
            @PathVariable Long eventId,
            @Valid @RequestBody NewCommentDto dto);

    @GetMapping("/{commentId}")
    CommentDto getComment(@PathVariable Long commentId);

    @GetMapping
    List<CommentDto> getComments(
            @PathVariable Long eventId,
            @RequestParam(defaultValue = "0") Integer from,
            @RequestParam(defaultValue = "10") Integer size);

    @PatchMapping("/{commentId}")
    CommentDto updateComment(
            @PathVariable Long userId,
            @PathVariable Long commentId,
            @Valid @RequestBody UpdateCommentByAuthorRequest request);

    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(
            @PathVariable Long userId,
            @PathVariable Long commentId);
}
