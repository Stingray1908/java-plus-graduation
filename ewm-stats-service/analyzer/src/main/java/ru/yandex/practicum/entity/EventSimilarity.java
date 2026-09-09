// EventSimilarity.java
package ru.yandex.practicum.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "event_similarity",
        uniqueConstraints = @UniqueConstraint(columnNames = {"event_a", "event_b"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class EventSimilarity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_a")
    private Long eventA;

    @Column(name = "event_b")
    private Long eventB;

    private Double score;
}

