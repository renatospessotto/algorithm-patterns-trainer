package com.appleetcode.backend.seed;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** DTOs de leitura de seed_problems.json / seed_topics.json (ver tools/seed-generation/generate_seed.py). */
public final class SeedDtos {

    private SeedDtos() {
    }

    public record SeedProblemsFile(List<PatternDto> patterns, List<ProblemDto> problems) {
    }

    public record PatternDto(
            String slug,
            String name,
            String description,
            @JsonProperty("order_in_plan") Integer orderInPlan
    ) {
    }

    public record ProblemDto(
            @JsonProperty("external_slug") String externalSlug,
            String title,
            @JsonProperty("leetcode_url") String leetcodeUrl,
            @JsonProperty("difficulty_label") String difficultyLabel,
            @JsonProperty("difficulty_rating") Double difficultyRating,
            @JsonProperty("is_core") boolean isCore,
            List<PatternRefDto> patterns,
            @JsonProperty("core_pattern_slug") String corePatternSlug,
            @JsonProperty("core_order") Integer coreOrder
    ) {
    }

    public record PatternRefDto(String slug, @JsonProperty("is_primary") boolean isPrimary) {
    }

    public record SeedTopicsFile(List<TopicDto> topics) {
    }

    public record TopicDto(String name, @JsonProperty("search_query") String searchQuery) {
    }
}
