package com.appleetcode.backend.src.service;

import com.appleetcode.backend.src.entity.Attempt;
import com.appleetcode.backend.src.entity.Problem;
import com.appleetcode.backend.src.entity.ReviewState;
import com.appleetcode.backend.src.repository.AttemptRepository;
import com.appleetcode.backend.src.repository.ProblemRepository;
import com.appleetcode.backend.src.repository.ReviewStateRepository;
import com.appleetcode.backend.config.AppClock;
import com.appleetcode.backend.src.controller.dto.AttemptHistoryItemDto;
import com.appleetcode.backend.src.controller.dto.AttemptRequest;
import com.appleetcode.backend.src.controller.dto.AttemptResponse;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Orquestra o registro de uma tentativa: grava o log em `attempts` e
 * recalcula `review_state` pro problema (POST /attempts, docs/spec/original-spec.md seção 6).
 */
@Service
public class AttemptsService {

    private final ProblemRepository problemRepository;
    private final AttemptRepository attemptRepository;
    private final ReviewStateRepository reviewStateRepository;
    private final ReviewSchedulerService reviewSchedulerService;
    private final AppClock appClock;

    public AttemptsService(
            ProblemRepository problemRepository,
            AttemptRepository attemptRepository,
            ReviewStateRepository reviewStateRepository,
            ReviewSchedulerService reviewSchedulerService,
            AppClock appClock
    ) {
        this.problemRepository = problemRepository;
        this.attemptRepository = attemptRepository;
        this.reviewStateRepository = reviewStateRepository;
        this.reviewSchedulerService = reviewSchedulerService;
        this.appClock = appClock;
    }

    @Transactional
    public AttemptResponse record(AttemptRequest request) {
        Problem problem = problemRepository.findById(request.problemId())
                .orElseThrow(() -> new EntityNotFoundException("Problem não encontrado: " + request.problemId()));

        Attempt attempt = new Attempt();
        attempt.setProblem(problem);
        attempt.setAttemptedAt(Instant.now());
        attempt.setDurationSeconds(request.durationSeconds());
        attempt.setResult(request.result());
        attempt.setConfidence(request.confidence() == null ? null : request.confidence().shortValue());
        attempt.setNote(request.note());
        attempt = attemptRepository.save(attempt);

        ReviewState existing = reviewStateRepository.findById(problem.getId()).orElse(null);
        ReviewState updated = reviewSchedulerService.recordAttempt(
                existing, problem, request.result(), request.durationSeconds(), Instant.now()
        );
        updated = reviewStateRepository.save(updated);

        return AttemptResponse.from(attempt, updated);
    }

    /** Histórico de tentativas, mais recente primeiro -- alimenta a aba "Histórico" do frontend. */
    @Transactional(readOnly = true)
    public List<AttemptHistoryItemDto> history() {
        return attemptRepository.findAllWithProblemOrderByAttemptedAtDesc().stream()
                .map(a -> AttemptHistoryItemDto.from(a, appClock.zone()))
                .toList();
    }
}
