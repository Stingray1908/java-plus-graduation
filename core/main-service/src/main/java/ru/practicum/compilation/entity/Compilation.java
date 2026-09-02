package ru.practicum.compilation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "compilations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Compilation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false)
    private Boolean pinned;

    @Column(length = 1000)
    private String description;

    @ElementCollection
    @CollectionTable(
            name = "compilation_events",
            joinColumns = @JoinColumn(name = "compilation_id")
    )
    @Column(name = "event_id")
    private List<Long> eventIds = new ArrayList<>();

    public Compilation(String title, Boolean pinned, String description) {
        this.title = title;
        this.pinned = pinned;
        this.description = description;
    }
}
