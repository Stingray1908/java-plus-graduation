package ru.yandex.practicum.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.dto.comments.CommentDto;
import ru.yandex.practicum.dto.comments.NewCommentDto;
import ru.yandex.practicum.dto.comments.UpdateCommentByAuthorRequest;
import ru.yandex.practicum.service.CommentService;
import ru.yandex.practicum.feigns.comments.PrivateCommentFeign;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class PrivateCommentController implements PrivateCommentFeign {

    private final CommentService commentService;

    @Override
    public CommentDto addComment(
            @PathVariable Long userId,
            @PathVariable Long eventId,
            @Valid @RequestBody NewCommentDto dto) {

        log.info("Пользователь {} добавляет комментарий к событию {}", userId, eventId);
        CommentDto comment = commentService.addComment(userId, eventId, dto);
        log.info("Комментарий создан: {}", comment.getId());
        return comment;
    }

    @Override
    public CommentDto getComment(@PathVariable Long commentId) {
        log.info("Запрос на получение комментария: {}", commentId);
        return commentService.getCommentById(commentId);
    }

    @Override
    public List<CommentDto> getComments(
            @PathVariable Long eventId,
            @RequestParam(defaultValue = "0") Integer from,
            @RequestParam(defaultValue = "10") Integer size) {

        log.info("Запрос на комментарии события {}: from={}, size={}", eventId, from, size);
        return commentService.getCommentsByEventId(eventId, from, size);
    }

    @Override
    public CommentDto updateComment(
            @PathVariable Long userId,
            @PathVariable Long commentId,
            @Valid @RequestBody UpdateCommentByAuthorRequest request) {

        log.info("Пользователь {} обновляет комментарий {}", userId, commentId);
        return commentService.updateCommentByAuthor(userId, commentId, request);
    }

    @Override
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(
            @PathVariable Long userId,
            @PathVariable Long commentId) {

        log.info("Пользователь {} удаляет свой комментарий {}", userId, commentId);
        commentService.deleteCommentByAuthor(userId, commentId);
    }
}
