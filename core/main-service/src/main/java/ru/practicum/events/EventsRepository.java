package ru.practicum.events;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventsRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

    Optional<Event> findByIdAndState(Long id, EventState state);

    @Query("SELECT e FROM Event e WHERE " +
            "(:users IS NULL OR e.initiator.id IN :users) AND " +
            "(:states IS NULL OR e.state IN :states) AND " +
            "(:categories IS NULL OR e.category.id IN :categories) AND " +
            "(:rangeStart IS NULL OR e.eventDate >= :rangeStart) AND " +
            "(:rangeEnd IS NULL OR e.eventDate <= :rangeEnd)")
    List<Event> findByFilters(
            List<Long> users,
            List<EventState> states,
            List<Long> categories,
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd,
            Pageable pageable
    );

    @Query("SELECT e FROM Event e WHERE e.state = 'PUBLISHED' AND " +
            "(:text IS NULL OR " +
            "LOWER(e.annotation) LIKE LOWER(CONCAT('%', :text, '%')) OR " +
            "LOWER(e.description) LIKE LOWER(CONCAT('%', :text, '%'))) AND " +
            "(:categories IS NULL OR e.category.id IN :categories) AND " +
            "(:paid IS NULL OR e.paid = :paid) AND " +
            "(e.eventDate >= :rangeStart) AND " +
            "(:rangeEnd IS NULL OR e.eventDate <= :rangeEnd)")
    List<Event> findPublishedEvents(
            String text,
            List<Long> categories,
            Boolean paid,
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd,
            Pageable pageable
    );

    @Query(value = "SELECT * FROM events WHERE initiator_id = :userId " +
            "ORDER BY event_date DESC LIMIT :size OFFSET :offset",
            nativeQuery = true)
    List<Event> findAllByInitiatorIdWithOffset(
            @Param("userId") Long userId,
            @Param("offset") int offset,
            @Param("size") int size);

    @Query("SELECT e FROM Event e " +
            "WHERE e.state = :state " +
            "AND e.eventDate > :now " +
            "AND e.initiator.id IN (" +
            "    SELECT s.publisher.id FROM Subscription s WHERE s.subscriber.id = :subscriberId" +
            ") " +
            "ORDER BY e.eventDate DESC")
    List<Event> findActualPublishedEventsBySubscriberId(
            @Param("subscriberId") Long subscriberId,
            @Param("state") EventState state,
            @Param("now") LocalDateTime now,
            Pageable pageable);

    Optional<Event> findById(Long id);

    @Query("SELECT e FROM Event e WHERE e.requestModeration = :requestModeration AND e.state = :state")
    List<Event> findByRequestModerationAndState(
            @Param("requestModeration") Boolean requestModeration,
            @Param("state") EventState state,
            Pageable pageable
    );

    @Query("SELECT e " +
            "FROM Event e " +
            "WHERE e.initiator.id = :userId " +
            "AND (e.state = 'CANCELED' OR e.state = 'REJECTED') " +
            "AND e.requestModeration = false " +
            "ORDER BY e.eventDate DESC")
    List<Event> findUserModerationHistory(
            @Param("userId") Long userId,
            Pageable pageable
    );

}
