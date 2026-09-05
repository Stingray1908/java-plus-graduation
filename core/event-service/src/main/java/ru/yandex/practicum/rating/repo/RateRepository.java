package ru.yandex.practicum.rating.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.rating.entity.Rate;

import java.util.List;
import java.util.Optional;

@Repository
public interface RateRepository extends JpaRepository<Rate, Long> {

    @Query("SELECT r FROM Rate r WHERE r.event = :eventId AND r.user = :userId")
    Optional<Rate> findByEventIdAndUserId(@Param("eventId") Long eventId, @Param("userId") Long userId);


    @Query("SELECT r.event, " +
            "SUM(CASE WHEN r.isLike = true THEN 1 ELSE -1 END) " +
            "FROM Rate r " +
            "WHERE r.event IN :eventIds " +
            "GROUP BY r.event")
    List<Object[]> getRatingsForEvents(@Param("eventIds") List<Long> eventIds);

    // Подсчет рейтинга для одного события (чтобы не гонять массивы ради 1 ивента)
    @Query("SELECT COALESCE(SUM(CASE WHEN r.isLike = true THEN 1 ELSE -1 END), 0) " +
            "FROM Rate r " +
            "WHERE r.event = :eventId")
    Long getRatingForEvent(@Param("eventId") Long eventId);
}
