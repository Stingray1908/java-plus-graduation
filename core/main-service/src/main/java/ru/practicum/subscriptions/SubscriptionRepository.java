package ru.practicum.subscriptions;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    boolean existsBySubscriber_IdAndPublisher_Id(Long subscriberId, Long publisherId);

    @Modifying
    @Query("DELETE FROM Subscription s WHERE s.subscriber.id = :subscriberId AND s.publisher.id = :publisherId")
    int deleteBySubscriberIdAndPublisherId(
            @Param("subscriberId") Long subscriberId,
            @Param("publisherId") Long publisherId);
}
