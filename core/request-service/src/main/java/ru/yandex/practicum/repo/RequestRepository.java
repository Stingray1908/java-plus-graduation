package ru.yandex.practicum.repo;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ru.yandex.practicum.entity.ParticipationRequest;
import ru.yandex.practicum.enums.EventState;

import java.util.List;
import java.util.Optional;

public interface RequestRepository extends JpaRepository<ParticipationRequest, Long> {

    long countByEventIdAndStatus(Long eventId, EventState status);

    @Query("SELECT r.eventId, COUNT(r) " +
            "FROM ParticipationRequest r " +
            "WHERE r.eventId IN :eventIds AND r.status = :status " +
            "GROUP BY r.eventId")
    List<Object[]> countRequestsByEventIdsAndStatus(
            @Param("eventIds") List<Long> eventIds,
            @Param("status") EventState status
    );


    List<ParticipationRequest> findByEventIdAndStatus(Long eventId, EventState status);

    boolean existsByEventIdAndRequesterId(Long eventId, Long requesterId);

    List<ParticipationRequest> findByEventId(Long eventId);

    List<ParticipationRequest> findByRequesterId(Long requesterId);

    Optional<ParticipationRequest> findByIdAndRequesterId(Long requestId, Long requesterId);

    Optional<ParticipationRequest> findById(Long id);
}
