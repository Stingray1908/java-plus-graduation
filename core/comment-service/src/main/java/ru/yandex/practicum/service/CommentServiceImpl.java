package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.dto.comments.CommentDto;
import ru.yandex.practicum.dto.comments.NewCommentDto;
import ru.yandex.practicum.dto.comments.UpdateCommentByAuthorRequest;
import ru.yandex.practicum.dto.comments.UpdateCommentByModeratorRequest;
import ru.yandex.practicum.dto.events.EventFullDto;
import ru.yandex.practicum.dto.user.UserShortDto;
import ru.yandex.practicum.entity.Comment;
import ru.yandex.practicum.enums.CommentStatus;
import ru.yandex.practicum.error.exception.NotFoundException;
import ru.yandex.practicum.feigns.event.EventsPublicFeign;
import ru.yandex.practicum.feigns.user.UserAdminFeign;
import ru.yandex.practicum.mapper.CommentMapper;
import ru.yandex.practicum.repo.CommentRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final UserAdminFeign userAdminFeign;
    private final EventsPublicFeign eventsAdminFeign;

    @Transactional
    @Override
    public CommentDto addComment(Long userId, Long eventId, NewCommentDto dto) {

        UserShortDto user = getUserById(userId);
        getEventById(eventId);

        Comment comment = CommentMapper.toComment(dto, eventId, userId);
        Comment saved = commentRepository.save(comment);

        return CommentMapper.toCommentDto(saved, user.getName());
    }

    @Override
    @Transactional
    public CommentDto getCommentById(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment not found"));

        UserShortDto user = getUserById(comment.getAuthorId());
        return CommentMapper.toCommentDto(comment, user.getName());
    }

    @Override
    @Transactional
    public List<CommentDto> getCommentsByEventId(Long eventId, Integer from, Integer size) {
        PageRequest page = PageRequest.of(from / size, size);

        List<Comment> comments = commentRepository.findByEventIdAndStatus(
                eventId, CommentStatus.APPROVED, page
        );

        if (comments.isEmpty()) return List.of();

        List<Long> userIds = comments.stream()
                .map(Comment::getAuthorId)
                .toList();

        List<UserShortDto> users = userAdminFeign.getAllInIds(userIds);

        Map<Long, String> authorNameMap = users.stream()
                .collect(Collectors.toMap(UserShortDto::getId, UserShortDto::getName));

        return comments.stream()
                .map(comment -> {
                    String authorName = authorNameMap.get(comment.getAuthorId());
                    return CommentMapper.toCommentDto(comment, authorName);
                })
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    @Transactional
    public CommentDto updateCommentByAuthor(Long userId, Long commentId, UpdateCommentByAuthorRequest request) {
        Comment comment = commentRepository.findById(commentId).orElseThrow();

        if (!comment.getAuthorId().equals(userId)) {
            throw new IllegalArgumentException("Only author can edit comment");
        }
        if (comment.getStatus() != CommentStatus.PENDING && comment.getStatus() != CommentStatus.APPROVED) {
            throw new IllegalArgumentException("Cannot edit this comment");
        }

        if (request.getText() != null) {
            comment.setText(request.getText());
        }
        comment.setUpdatedOn(LocalDateTime.now());

        UserShortDto user = getUserById(commentId);
        return CommentMapper.toCommentDto(commentRepository.save(comment), user.getName());
    }

    @Override
    @Transactional
    public CommentDto updateCommentByModerator(Long userId, Long commentId, UpdateCommentByModeratorRequest request) {
        Comment comment = commentRepository.findById(commentId).orElseThrow();

        comment.setStatus(request.getStatus());
        if (request.getText() != null) {
            comment.setText(request.getText());
        }
        comment.setUpdatedOn(LocalDateTime.now());

        Comment saved = commentRepository.save(comment);
        log.info("Comment {} updated by moderator {}: status={}, text={}",
                saved.getId(), userId, saved.getStatus(), saved.getText());

        UserShortDto user = getUserById(commentId);
        return CommentMapper.toCommentDto(saved, user.getName());
    }

    @Transactional
    @Override
    public void deleteCommentByAuthor(Long userId, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment not found"));

        if (!comment.getAuthorId().equals(userId)) {
            throw new IllegalArgumentException("You can only delete your own comments");
        }
        if (comment.getStatus() == CommentStatus.REJECTED) {
            throw new IllegalArgumentException("Cannot delete rejected comment");
        }

        commentRepository.deleteById(commentId);
    }

    @Transactional
    @Override
    public void deleteCommentByModerator(Long userId, Long commentId) {
        commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment not found"));
        commentRepository.deleteById(commentId);
    }

    private UserShortDto getUserById(Long id) {
        UserShortDto user = userAdminFeign.getById(id);
        if (user == null) {
            throw new NotFoundException("пользователь не найден");
        }
        return user;
    }

    private EventFullDto getEventById(Long id) {
        ResponseEntity<EventFullDto> event = eventsAdminFeign.getEventByIdInside(id);
        if (!event.getStatusCode().is2xxSuccessful()) {
            throw new NotFoundException("событие не найдено");
        }
        return event.getBody();
    }
}
