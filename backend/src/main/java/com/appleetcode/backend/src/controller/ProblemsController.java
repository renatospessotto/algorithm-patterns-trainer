package com.appleetcode.backend.src.controller;

import com.appleetcode.backend.src.service.ProblemsService;
import com.appleetcode.backend.src.controller.dto.ProblemDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProblemsController {

    private final ProblemsService problemsService;

    public ProblemsController(ProblemsService problemsService) {
        this.problemsService = problemsService;
    }

    @GetMapping("/problems/random")
    public ResponseEntity<ProblemDto> random(
            @RequestParam(required = false) String pattern,
            @RequestParam(name = "exclude_attempted", defaultValue = "true") boolean excludeAttempted
    ) {
        return problemsService.random(pattern, excludeAttempted)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
