package ru.yandex.practicum.subscriptions;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    @Query("SELECT COUNT(s) > 0 FROM Subscription s WHERE s.subscriber = :subscriberId AND s.publisher = :publisherId")
    boolean existsBySubscriber_IdAndPublisher_Id(
            @Param("subscriberId") Long subscriberId,
            @Param("publisherId") Long publisherId);

    @Modifying
    @Query("DELETE FROM Subscription s WHERE s.subscriber = :subscriberId AND s.publisher = :publisherId")
    int deleteBySubscriberIdAndPublisherId(
            @Param("subscriberId") Long subscriberId,
            @Param("publisherId") Long publisherId);

    @Query("SELECT s.publisher FROM Subscription s WHERE s.subscriber = :subscriberId")
    List<Long> findPublishersBySubscriber(@Param("subscriberId") Long subscriberId);
}
