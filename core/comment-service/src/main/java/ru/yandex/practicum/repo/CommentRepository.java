package ru.yandex.practicum.repo;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.enums.CommentStatus;
import ru.yandex.practicum.entity.Comment;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByEventIdAndStatus(Long eventId, CommentStatus status, Pageable pageable);

    List<Comment> findByEventId(Long eventId, Pageable pageable);

    boolean existsByEventIdAndAuthorIdAndStatusIn(
            Long eventId, Long userId, List<CommentStatus> statuses);
}
