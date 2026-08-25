package ru.practicum.comments.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.comments.*;
import ru.practicum.error.exception.NotFoundException;
import ru.practicum.events.Event;
import ru.practicum.events.EventsRepository;
import ru.practicum.user.User;
import ru.practicum.user.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final EventsRepository eventsRepository;

    @Transactional
    @Override
    public CommentDto addComment(Long userId, Long eventId, NewCommentDto dto) {
        Optional<User> author = userRepository.findById(userId);
        Event event = eventsRepository.getReferenceById(eventId);

        Comment comment = CommentMapper.toComment(dto, event, author.orElseThrow());
        Comment saved = commentRepository.save(comment);

        return CommentMapper.toCommentDto(saved);
    }

    @Override
    @Transactional
    public CommentDto getCommentById(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment not found"));
        return CommentMapper.toCommentDto(comment);
    }

    @Override
    @Transactional
    public List<CommentDto> getCommentsByEventId(Long eventId, Integer from, Integer size) {
        PageRequest page = PageRequest.of(from / size, size);
        return commentRepository.findByEventIdAndStatus(eventId, CommentStatus.APPROVED, page).stream()
                .map(CommentMapper::toCommentDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CommentDto updateCommentByAuthor(Long userId, Long commentId, UpdateCommentByAuthorRequest request) {
        Comment comment = commentRepository.findById(commentId).orElseThrow();

        if (!comment.getAuthor().getId().equals(userId)) {
            throw new IllegalArgumentException("Only author can edit comment");
        }
        if (comment.getStatus() != CommentStatus.PENDING && comment.getStatus() != CommentStatus.APPROVED) {
            throw new IllegalArgumentException("Cannot edit this comment");
        }

        if (request.getText() != null) {
            comment.setText(request.getText());
        }
        comment.setUpdatedOn(LocalDateTime.now());

        return CommentMapper.toCommentDto(commentRepository.save(comment));
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

        return CommentMapper.toCommentDto(saved);
    }

    @Transactional
    @Override
    public void deleteCommentByAuthor(Long userId, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment not found"));

        if (!comment.getAuthor().getId().equals(userId)) {
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
}
