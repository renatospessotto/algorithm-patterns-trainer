package com.appleetcode.backend.src.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Subconjunto dos campos usados da API pública do Semantic Scholar (GET /graph/v1/paper/search). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SemanticScholarPaperDto(
        String title,
        String url,
        Integer year,
        @JsonProperty("citationCount") Integer citationCount,
        String venue,
        List<AuthorDto> authors
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AuthorDto(String name) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SearchResponse(List<SemanticScholarPaperDto> data) {
    }
}
