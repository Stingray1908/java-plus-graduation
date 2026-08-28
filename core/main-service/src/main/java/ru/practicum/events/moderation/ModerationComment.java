package ru.practicum.events.moderation;

import jakarta.persistence.*;
import lombok.*;
import ru.practicum.events.Event;
import java.time.LocalDateTime;

@Entity
@Table(name = "moderation_comments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModerationComment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "comment_text", nullable = false, length = 2000)
    private String commentText;

    @Column(name = "created_on", nullable = false)
    private LocalDateTime createdOn = LocalDateTime.now();
}

