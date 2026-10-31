package com.appleetcode.backend.src.entity;

import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class ProblemPatternId implements Serializable {

    private Long problemId;
    private Long patternId;

    public ProblemPatternId(Long problemId, Long patternId) {
        this.problemId = problemId;
        this.patternId = patternId;
    }
}
