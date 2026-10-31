package com.appleetcode.backend.src.controller;

import com.appleetcode.backend.src.service.ReviewQueueService;
import com.appleetcode.backend.src.controller.dto.ReviewQueueItemDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class QueueController {

    private final ReviewQueueService reviewQueueService;

    public QueueController(ReviewQueueService reviewQueueService) {
        this.reviewQueueService = reviewQueueService;
    }

    @GetMapping("/queue/review")
    public List<ReviewQueueItemDto> reviewQueue() {
        return reviewQueueService.currentQueueDto();
    }
}
