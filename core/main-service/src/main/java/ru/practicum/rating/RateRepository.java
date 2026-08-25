package ru.practicum.rating;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RateRepository extends JpaRepository<Rate, Long> {

    Optional<Rate> findByEventIdAndUserId(Long eventId, Long userId);

    @Query("SELECT r.event.id, " +
            "SUM(CASE WHEN r.isLike = true THEN 1 ELSE -1 END) " +
            "FROM Rate r " +
            "WHERE r.event.id IN :eventIds " +
            "GROUP BY r.event.id")
    List<Object[]> getRatingsForEvents(@Param("eventIds") List<Long> eventIds);

    // Подсчет рейтинга для одного события (чтобы не гонять массивы ради 1 ивента)
    @Query("SELECT COALESCE(SUM(CASE WHEN r.isLike = true THEN 1 ELSE -1 END), 0) " +
            "FROM Rate r " +
            "WHERE r.event.id = :eventId")
    Long getRatingForEvent(@Param("eventId") Long eventId);
}