package com.appleetcode.backend.src.controller.dto;

import com.appleetcode.backend.src.entity.Attempt;
import com.appleetcode.backend.src.entity.ReviewState;

import java.time.Instant;

public record AttemptResponse(
        Long attemptId,
        Long problemId,
        double stabilityDays,
        double emaQuality,
        Instant lastSeenAt,
        int attemptsCount,
        int successesCount
) {
    public static AttemptResponse from(Attempt attempt, ReviewState state) {
        return new AttemptResponse(
                attempt.getId(), attempt.getProblem().getId(),
                state.getStabilityDays(), state.getEmaQuality(), state.getLastSeenAt(),
                state.getAttemptsCount(), state.getSuccessesCount()
        );
    }
}
