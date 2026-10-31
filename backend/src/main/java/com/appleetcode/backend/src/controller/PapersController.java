package com.appleetcode.backend.src.controller;

import com.appleetcode.backend.src.service.PapersService;
import com.appleetcode.backend.src.controller.dto.PaperDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class PapersController {

    private final PapersService papersService;

    public PapersController(PapersService papersService) {
        this.papersService = papersService;
    }

    @GetMapping("/papers/feed")
    public List<PaperDto> feed(@RequestParam(required = false) String topic) {
        return papersService.feed(topic);
    }
}
