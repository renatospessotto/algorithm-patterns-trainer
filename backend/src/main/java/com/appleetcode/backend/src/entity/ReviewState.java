package com.appleetcode.backend.src.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Persistable;

import java.time.Instant;

/**
 * Implementa {@link Persistable} porque o Id é atribuído manualmente (via
 * {@code @MapsId}, chave estrangeira compartilhada com `problems`), não
 * gerado pelo banco. Sem isso, o Spring Data JPA vê um Id não-nulo logo na
 * criação e assume que a entidade já existe, chamando `merge()` em vez de
 * `persist()` — e o `merge()` falha (AssertionFailure: null identifier) pra
 * um review_state genuinamente novo, porque a linha ainda não existe pra
 * mesclar.
 */
@Entity
@Table(name = "review_state")
@Getter
@Setter
@NoArgsConstructor
public class ReviewState implements Persistable<Long> {

    @Id
    @Column(name = "problem_id")
    private Long problemId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "problem_id")
    private Problem problem;

    @Transient
    private boolean isNew = true;

    @Column(name = "stability_days", nullable = false)
    private double stabilityDays;

    @Column(name = "ema_quality", nullable = false)
    private double emaQuality;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    @Column(name = "attempts_count", nullable = false)
    private int attemptsCount;

    @Column(name = "successes_count", nullable = false)
    private int successesCount;

    public ReviewState(Problem problem) {
        this.problem = problem;
        this.problemId = problem.getId();
    }

    @Override
    public Long getId() {
        return problemId;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostPersist
    @PostLoad
    void markNotNew() {
        this.isNew = false;
    }
}
