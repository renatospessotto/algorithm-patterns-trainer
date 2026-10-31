package com.appleetcode.backend.src.controller;

import com.appleetcode.backend.src.service.PatternsService;
import com.appleetcode.backend.src.controller.dto.PatternProgressDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class PatternsController {

    private final PatternsService patternsService;

    public PatternsController(PatternsService patternsService) {
        this.patternsService = patternsService;
    }

    @GetMapping("/patterns")
    public List<PatternProgressDto> patterns() {
        return patternsService.progress();
    }
}
