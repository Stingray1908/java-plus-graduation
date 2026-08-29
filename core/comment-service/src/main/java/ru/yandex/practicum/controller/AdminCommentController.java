package ru.yandex.practicum.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.dto.comments.CommentDto;
import ru.yandex.practicum.dto.comments.UpdateCommentByModeratorRequest;
import ru.yandex.practicum.service.CommentService;
import ru.yandex.practicum.feigns.comments.AdminCommentFeign;

import java.util.List;

@RestController
@RequestMapping("/admin/comments")
@RequiredArgsConstructor
@Slf4j
public class AdminCommentController implements AdminCommentFeign {

    private final CommentService commentService;

    @Override
    public List<CommentDto> getAllComments(
            @RequestParam(required = false) Long eventId,
            @RequestParam(defaultValue = "0") Integer from,
            @RequestParam(defaultValue = "10") Integer size) {
        log.info("Админ запрашивает комментарии");
        return commentService.getCommentsByEventId(eventId, from, size);
    }

    @Override
    public CommentDto moderateComment(
            @PathVariable Long commentId,
            @Valid @RequestBody UpdateCommentByModeratorRequest request) {

        log.info("Модерация комментария {}: статус={}", commentId, request.getStatus());
        return commentService.updateCommentByModerator(null, commentId, request);
    }

    @Override
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCommentByAdmin(@PathVariable Long commentId) {
        log.info("Админ удаляет комментарий: {}", commentId);
        commentService.deleteCommentByModerator(null, commentId);
    }
}
