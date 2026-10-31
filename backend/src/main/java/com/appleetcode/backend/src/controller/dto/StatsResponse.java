package com.appleetcode.backend.src.controller.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record StatsResponse(
        long totalAttempts,
        Map<String, Double> avgDurationSecondsByDifficulty,
        List<HeatmapDay> heatmap
) {
    public record HeatmapDay(LocalDate date, long count) {
    }
}
