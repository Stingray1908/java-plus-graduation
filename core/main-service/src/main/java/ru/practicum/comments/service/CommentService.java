package ru.practicum.comments.service;

import ru.practicum.comments.CommentDto;
import ru.practicum.comments.NewCommentDto;
import ru.practicum.comments.UpdateCommentByAuthorRequest;
import ru.practicum.comments.UpdateCommentByModeratorRequest;

import java.util.List;

public interface CommentService {
    CommentDto addComment(Long userId, Long eventId, NewCommentDto dto);

    CommentDto getCommentById(Long commentId);

    List<CommentDto> getCommentsByEventId(Long eventId, Integer from, Integer size);

    CommentDto updateCommentByAuthor(Long userId, Long commentId, UpdateCommentByAuthorRequest request);

    CommentDto updateCommentByModerator(Long userId, Long commentId, UpdateCommentByModeratorRequest request);

    void deleteCommentByAuthor(Long userId, Long commentId);

    void deleteCommentByModerator(Long userId, Long commentId);
}
