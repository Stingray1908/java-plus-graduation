package ru.yandex.practicum.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.dto.comments.CommentDto;
import ru.yandex.practicum.enums.CommentStatus;
import ru.yandex.practicum.dto.comments.NewCommentDto;
import ru.yandex.practicum.entity.Comment;

import java.time.LocalDateTime;

@Component
public class CommentMapper {

    public static CommentDto toCommentDto(Comment comment, String author) {
        if (comment == null || author.isEmpty()) return null;
        return CommentDto.builder()
                .id(comment.getId())
                .text(comment.getText())
                .eventId(comment.getEventId())
                .authorId(comment.getAuthorId())
                .authorName(author)
                .status(comment.getStatus())
                .createdOn(comment.getCreatedOn())
                .updatedOn(comment.getUpdatedOn())
                .build();
    }

    public static Comment toComment(NewCommentDto dto, Long eventId, Long authorId) {
        if (dto == null || eventId == null || authorId == null) return null;
        Comment comment = new Comment();
        comment.setText(dto.getText());
        comment.setEventId(eventId);
        comment.setAuthorId(authorId);
        comment.setStatus(CommentStatus.PENDING);
        comment.setCreatedOn(LocalDateTime.now());
        return comment;
    }
}
