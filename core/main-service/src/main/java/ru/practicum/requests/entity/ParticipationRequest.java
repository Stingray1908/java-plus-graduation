package ru.practicum.requests.entity;

import jakarta.persistence.*;
import lombok.*;
import ru.yandex.practicum.enums.EventState;
import ru.practicum.events.entity.Event;
import ru.practicum.user.User;

import java.time.LocalDateTime;

@Entity
@Table(name = "requests", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"event_id", "requester_id"}, name = "unique_requester_per_event")
})
@ToString
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created", nullable = false)
    private LocalDateTime created;

    @Column(nullable = false)
    private Long eventId;

    @Column(nullable = false)
    private Long requesterId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private EventState status;
}
