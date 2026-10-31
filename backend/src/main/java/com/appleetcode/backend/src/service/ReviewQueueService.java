package com.appleetcode.backend.src.service;

import com.appleetcode.backend.src.controller.dto.ReviewQueueItemDto;
import com.appleetcode.backend.src.repository.ReviewStateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/**
 * Fila de prioridade calculada na hora (docs/spec/original-spec.md seção 4): sem conceito de
 * "atrasado", só ordem — urgência decrescente, empate quebrado por
 * stability_days menor primeiro (o mais frágil primeiro).
 */
@Service
public class ReviewQueueService {

    private final ReviewStateRepository reviewStateRepository;
    private final ReviewSchedulerService reviewSchedulerService;

    public ReviewQueueService(ReviewStateRepository reviewStateRepository, ReviewSchedulerService reviewSchedulerService) {
        this.reviewStateRepository = reviewStateRepository;
        this.reviewSchedulerService = reviewSchedulerService;
    }

    @Transactional(readOnly = true)
    public List<UrgentReviewItem> currentQueue() {
        Instant now = Instant.now();
        return reviewStateRepository.findAllWithProblem().stream()
                .map(state -> new UrgentReviewItem(state.getProblem(), state, reviewSchedulerService.calculateUrgency(state, now)))
                .sorted(
                        Comparator.comparingDouble(UrgentReviewItem::urgency).reversed()
                                .thenComparingDouble(item -> item.state().getStabilityDays())
                )
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReviewQueueItemDto> currentQueueDto() {
        return currentQueue().stream().map(ReviewQueueItemDto::from).toList();
    }
}
