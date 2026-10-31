package com.appleetcode.backend.src.controller;

import com.appleetcode.backend.src.service.StatsService;
import com.appleetcode.backend.src.controller.dto.StatsResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/stats")
    public StatsResponse stats() {
        return statsService.stats();
    }
}
