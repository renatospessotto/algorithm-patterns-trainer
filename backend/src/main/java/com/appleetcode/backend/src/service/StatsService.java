package com.appleetcode.backend.src.service;

import com.appleetcode.backend.config.AppClock;
import com.appleetcode.backend.src.controller.dto.StatsResponse;
import com.appleetcode.backend.src.entity.Attempt;
import com.appleetcode.backend.src.repository.AttemptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** Histórico geral: tempo médio por dificuldade e heatmap de dias com sessão feita (docs/spec/original-spec.md seção 6). */
@Service
public class StatsService {

    private final AttemptRepository attemptRepository;
    private final AppClock appClock;

    public StatsService(AttemptRepository attemptRepository, AppClock appClock) {
        this.attemptRepository = attemptRepository;
        this.appClock = appClock;
    }

    @Transactional(readOnly = true)
    public StatsResponse stats() {
        List<Attempt> attempts = attemptRepository.findAllByOrderByAttemptedAtAsc();

        Map<String, Double> avgDurationByDifficulty = attempts.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getProblem().getDifficultyLabel(),
                        Collectors.averagingInt(Attempt::getDurationSeconds)
                ));

        Map<LocalDate, Long> countsByDate = new TreeMap<>(attempts.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getAttemptedAt().atZone(appClock.zone()).toLocalDate(),
                        Collectors.counting()
                )));

        List<StatsResponse.HeatmapDay> heatmap = countsByDate.entrySet().stream()
                .map(e -> new StatsResponse.HeatmapDay(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(StatsResponse.HeatmapDay::date))
                .toList();

        return new StatsResponse(attempts.size(), avgDurationByDifficulty, heatmap);
    }
}
