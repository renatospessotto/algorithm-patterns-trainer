package com.appleetcode.backend.src.controller.dto;

import com.appleetcode.backend.src.entity.Problem;

public record ProblemDto(
        Long id,
        String externalSlug,
        String title,
        String leetcodeUrl,
        String difficultyLabel,
        Double difficultyRating,
        boolean isCore
) {
    public static ProblemDto from(Problem p) {
        return new ProblemDto(
                p.getId(), p.getExternalSlug(), p.getTitle(), p.getLeetcodeUrl(),
                p.getDifficultyLabel(), p.getDifficultyRating(), p.isCore()
        );
    }
}
