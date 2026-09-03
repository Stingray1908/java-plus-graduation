package ru.practicum.events.moderation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ModerationCommentRepository extends JpaRepository<ModerationComment, Long> {

    @Query("SELECT mc FROM ModerationComment mc " +
            "WHERE mc.event.id IN :eventIds " +
            "AND mc.createdOn = (" +
            "SELECT MAX(mc2.createdOn) " +
            "FROM ModerationComment mc2 " +
            "WHERE mc2.event.id = mc.event.id" +
            ")")
    List<ModerationComment> findLastCommentsByEventIds(@Param("eventIds") List<Long> eventIds);
}
