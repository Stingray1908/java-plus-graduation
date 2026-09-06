package ru.yandex.practicum.rating.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ratings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Rate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long event;

    @Column(name = "user_id", nullable = false)
    private Long user;

    @Column(name = "is_like", nullable = false)
    private Boolean isLike;
}
