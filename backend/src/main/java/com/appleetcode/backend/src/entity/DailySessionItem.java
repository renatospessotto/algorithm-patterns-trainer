package com.appleetcode.backend.src.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Um slot já decidido e fixado da sessão de um `day_number`. Uma vez gravado,
 * GET /today sempre devolve o mesmo problema pra aquele slot naquele dia,
 * resolvido ou não -- ver V3__daily_session_items.sql.
 */
@Entity
@Table(name = "daily_session_items")
@Getter
@Setter
@NoArgsConstructor
public class DailySessionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "day_number", nullable = false)
    private Integer dayNumber;

    @Column(name = "slot_index", nullable = false)
    private Integer slotIndex;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "problem_id")
    private Problem problem;

    @Column(nullable = false, length = 20)
    private String origin;

    @Column
    private Double urgency;

    public DailySessionItem(Integer dayNumber, Integer slotIndex, Problem problem, String origin, Double urgency) {
        this.dayNumber = dayNumber;
        this.slotIndex = slotIndex;
        this.problem = problem;
        this.origin = origin;
        this.urgency = urgency;
    }
}
