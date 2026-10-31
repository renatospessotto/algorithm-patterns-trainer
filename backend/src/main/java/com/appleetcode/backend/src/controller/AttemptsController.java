package com.appleetcode.backend.src.controller;

import com.appleetcode.backend.src.service.AttemptsService;
import com.appleetcode.backend.src.controller.dto.AttemptHistoryItemDto;
import com.appleetcode.backend.src.controller.dto.AttemptRequest;
import com.appleetcode.backend.src.controller.dto.AttemptResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class AttemptsController {

    private final AttemptsService attemptsService;

    public AttemptsController(AttemptsService attemptsService) {
        this.attemptsService = attemptsService;
    }

    @PostMapping("/attempts")
    @ResponseStatus(HttpStatus.CREATED)
    public AttemptResponse record(@Valid @RequestBody AttemptRequest request) {
        return attemptsService.record(request);
    }

    @GetMapping("/attempts")
    public List<AttemptHistoryItemDto> history() {
        return attemptsService.history();
    }
}
