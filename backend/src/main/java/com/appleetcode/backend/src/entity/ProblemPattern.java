package com.appleetcode.backend.src.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "problem_patterns")
@Getter
@Setter
@NoArgsConstructor
public class ProblemPattern {

    @EmbeddedId
    private ProblemPatternId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("problemId")
    @JoinColumn(name = "problem_id")
    private Problem problem;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("patternId")
    @JoinColumn(name = "pattern_id")
    private Pattern pattern;

    @Column(name = "is_primary", nullable = false)
    private boolean primary;

    public ProblemPattern(Problem problem, Pattern pattern, boolean primary) {
        this.problem = problem;
        this.pattern = pattern;
        this.primary = primary;
        this.id = new ProblemPatternId(problem.getId(), pattern.getId());
    }
}
