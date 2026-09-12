package ru.yandex.practicum.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.yandex.practicum.entity.UserAction;

import java.util.List;
import java.util.Optional;

public interface UserActionRepository extends JpaRepository<UserAction, Long> {

    List<UserAction> findByUserId(Long userId);

    boolean existsByUserIdAndEventId(Long userId, Long eventId);

    @Query("SELECT ua FROM UserAction ua WHERE ua.eventId IN :eventIds")
    List<UserAction> findByEventIdIn(@Param("eventIds") List<Long> eventIds);

    Optional<UserAction> findByUserIdAndEventId(Long userId, Long eventId);

}
