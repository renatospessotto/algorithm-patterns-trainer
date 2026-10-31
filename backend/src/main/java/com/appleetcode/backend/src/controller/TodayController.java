package com.appleetcode.backend.src.controller;

import com.appleetcode.backend.src.service.SessionPlannerService;
import com.appleetcode.backend.src.controller.dto.TodayResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TodayController {

    private final SessionPlannerService sessionPlannerService;

    public TodayController(SessionPlannerService sessionPlannerService) {
        this.sessionPlannerService = sessionPlannerService;
    }

    @GetMapping("/today")
    public TodayResponse today() {
        return sessionPlannerService.today();
    }
}
