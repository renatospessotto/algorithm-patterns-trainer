package com.appleetcode.backend.src.controller.dto;

import com.appleetcode.backend.src.service.UrgentReviewItem;

import java.time.Instant;

public record ReviewQueueItemDto(
        ProblemDto problem,
        double urgency,
        double stabilityDays,
        double emaQuality,
        Instant lastSeenAt,
        int attemptsCount,
        int successesCount
) {
    public static ReviewQueueItemDto from(UrgentReviewItem item) {
        return new ReviewQueueItemDto(
                ProblemDto.from(item.problem()),
                item.urgency(),
                item.state().getStabilityDays(),
                item.state().getEmaQuality(),
                item.state().getLastSeenAt(),
                item.state().getAttemptsCount(),
                item.state().getSuccessesCount()
        );
    }
}
