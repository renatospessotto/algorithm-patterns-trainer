package com.appleetcode.backend.src.service;

import com.appleetcode.backend.src.controller.dto.PatternProgressDto;
import com.appleetcode.backend.src.entity.Pattern;
import com.appleetcode.backend.src.repository.PatternRepository;
import com.appleetcode.backend.src.repository.ProblemPatternRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class PatternsService {

    private final PatternRepository patternRepository;
    private final ProblemPatternRepository problemPatternRepository;

    public PatternsService(PatternRepository patternRepository, ProblemPatternRepository problemPatternRepository) {
        this.patternRepository = patternRepository;
        this.problemPatternRepository = problemPatternRepository;
    }

    @Transactional(readOnly = true)
    public List<PatternProgressDto> progress() {
        List<Pattern> patterns = patternRepository.findAll();
        patterns.sort(Comparator.comparing(Pattern::getOrderInPlan, Comparator.nullsLast(Integer::compareTo)));

        return patterns.stream()
                .map(p -> new PatternProgressDto(
                        p.getId(),
                        p.getSlug(),
                        p.getName(),
                        p.getOrderInPlan(),
                        problemPatternRepository.countCoreByPattern(p.getId()),
                        problemPatternRepository.countCoreSolvedByPattern(p.getId()),
                        problemPatternRepository.avgEmaQualityByPattern(p.getId())
                ))
                .toList();
    }
}
