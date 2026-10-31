package com.appleetcode.backend.src.controller.dto;

public record PatternProgressDto(
        Long id,
        String slug,
        String name,
        Integer orderInPlan,
        long coreTotal,
        long coreSolved,
        Double avgEmaQuality
) {
}
