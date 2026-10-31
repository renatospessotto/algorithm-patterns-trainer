package com.appleetcode.backend.src.controller.dto;

import com.appleetcode.backend.src.entity.AttemptResult;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AttemptRequest(
        @NotNull Long problemId,
        @NotNull @Positive Integer durationSeconds,
        @NotNull AttemptResult result,
        @Min(1) @Max(5) Integer confidence,
        String note
) {
}
