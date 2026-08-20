package ru.practicum.comments;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByEventIdAndStatus(Long eventId, CommentStatus status, Pageable pageable);

    List<Comment> findByEventId(Long eventId, Pageable pageable);

    @Query("SELECT COUNT(c) > 0 FROM Comment c " +
            "WHERE c.event.id = :eventId " +
            "AND c.author.id = :userId " +
            "AND c.status IN :statuses")
    boolean existsByEventIdAndAuthorIdAndStatusIn(
            @Param("eventId") Long eventId,
            @Param("userId") Long userId,
            @Param("statuses") List<CommentStatus> statuses);
}
