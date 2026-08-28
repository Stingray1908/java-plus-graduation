package ru.practicum.comments.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.comments.CommentDto;
import ru.practicum.comments.UpdateCommentByModeratorRequest;
import ru.practicum.comments.service.CommentService;

import java.util.List;

@RestController
@RequestMapping("/admin/comments")
@RequiredArgsConstructor
@Slf4j
public class AdminCommentController {

    private final CommentService commentService;

    @GetMapping
    public List<CommentDto> getAllComments(
            @RequestParam(required = false) Long eventId,
            @RequestParam(defaultValue = "0") Integer from,
            @RequestParam(defaultValue = "10") Integer size) {
        log.info("Админ запрашивает комментарии");
        return commentService.getCommentsByEventId(eventId, from, size);
    }

    @PatchMapping("/{commentId}")
    public CommentDto moderateComment(
            @PathVariable Long commentId,
            @Valid @RequestBody UpdateCommentByModeratorRequest request) {

        log.info("Модерация комментария {}: статус={}", commentId, request.getStatus());
        return commentService.updateCommentByModerator(null, commentId, request);
    }

    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCommentByAdmin(@PathVariable Long commentId) {
        log.info("Админ удаляет комментарий: {}", commentId);
        commentService.deleteCommentByModerator(null, commentId);
    }
}