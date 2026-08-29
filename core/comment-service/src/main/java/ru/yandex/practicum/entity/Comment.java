package ru.yandex.practicum.entity;

import jakarta.persistence.*;
import lombok.*;
import ru.yandex.practicum.enums.CommentStatus;
import ru.practicum.events.entity.Event;
import ru.practicum.user.User;

import java.time.LocalDateTime;

@Entity
@Table(name = "comments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 1000)
    private String text;

    @Column(nullable = false)
    private Long eventId; //убраны сущности

    @Column(nullable = false)
    private Long authorId; //убраны сущности

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CommentStatus status; // PENDING, APPROVED, REJECTED

    @Column(name = "created_on", nullable = false)
    private LocalDateTime createdOn;

    @Column(name = "updated_on")
    private LocalDateTime updatedOn;
}
