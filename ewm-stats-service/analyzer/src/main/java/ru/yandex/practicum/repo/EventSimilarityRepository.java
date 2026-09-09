package ru.yandex.practicum.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.entity.EventSimilarity;

import java.util.List;

public interface EventSimilarityRepository extends JpaRepository<EventSimilarity, Long> {

    @Query("SELECT e FROM EventSimilarity e WHERE e.eventA = :id OR e.eventB = :id ORDER BY e.score DESC")
    List<EventSimilarity> findByEventIdOrderByScoreDesc(@Param("id") Long eventId);

    @Query("SELECT e FROM EventSimilarity e WHERE e.eventA = :id OR e.eventB = :id")
    List<EventSimilarity> findByEventId(@Param("id") Long eventId);

    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO event_similarity (event_a, event_b, score)
            VALUES (:a, :b, :score)
            ON CONFLICT (event_a, event_b) DO UPDATE SET score = :score
            """, nativeQuery = true)
    void upsertScore(@Param("a") long eventA, @Param("b") long eventB, @Param("score") double score);

}
