package com.appleetcode.backend.src.controller.dto;

import com.appleetcode.backend.src.entity.Paper;

import java.time.Instant;

public record PaperDto(
        Long id,
        String title,
        String url,
        String source,
        String venue,
        Integer publicationYear,
        Integer citationCount,
        String authors,
        Instant cachedAt
) {
    public static PaperDto from(Paper p) {
        return new PaperDto(
                p.getId(), p.getTitle(), p.getUrl(), p.getSource(), p.getVenue(),
                p.getPublicationYear(), p.getCitationCount(), p.getAuthors(), p.getCachedAt()
        );
    }
}
