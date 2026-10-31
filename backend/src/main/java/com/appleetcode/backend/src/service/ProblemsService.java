package com.appleetcode.backend.src.service;

import com.appleetcode.backend.src.controller.dto.ProblemDto;
import com.appleetcode.backend.src.entity.Problem;
import com.appleetcode.backend.src.repository.ProblemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Random;

/** Modo exploração (docs/spec/original-spec.md seções 5 e 6): puxa do pool (is_core=false), priorizando não tentados. */
@Service
public class ProblemsService {

    private final ProblemRepository problemRepository;
    private final Random random = new Random();

    public ProblemsService(ProblemRepository problemRepository) {
        this.problemRepository = problemRepository;
    }

    @Transactional(readOnly = true)
    public Optional<ProblemDto> random(String pattern, boolean excludeAttempted) {
        List<Problem> candidates = pickCandidates(pattern, excludeAttempted);
        if (candidates.isEmpty() && excludeAttempted) {
            // todo mundo do padrão já foi tentado ao menos uma vez -- cai pro pool completo do padrão
            candidates = pickCandidates(pattern, false);
        }
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        Problem chosen = candidates.get(random.nextInt(candidates.size()));
        return Optional.of(ProblemDto.from(chosen));
    }

    private List<Problem> pickCandidates(String pattern, boolean excludeAttempted) {
        if (pattern != null && !pattern.isBlank()) {
            return excludeAttempted
                    ? problemRepository.findUnattemptedPoolByPattern(pattern)
                    : problemRepository.findPoolByPattern(pattern);
        }
        return excludeAttempted ? problemRepository.findUnattemptedPool() : problemRepository.findPool();
    }
}
