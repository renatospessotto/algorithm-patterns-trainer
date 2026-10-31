package com.appleetcode.backend.src.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "attempts")
@Getter
@Setter
@NoArgsConstructor
public class Attempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "problem_id")
    private Problem problem;

    @Column(name = "attempted_at", nullable = false)
    private Instant attemptedAt = Instant.now();

    @Column(name = "duration_seconds", nullable = false)
    private Integer durationSeconds;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AttemptResult result;

    @Column
    private Short confidence;

    @Column(columnDefinition = "text")
    private String note;
}
