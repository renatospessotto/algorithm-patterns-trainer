package com.appleetcode.backend.src.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Paper acadêmico em cache pra aba "Leitura" (banco de dados, IA, ML
 * tradicional etc). Ordenado por citation_count como proxy de
 * "bem-sucedido" -- não existe fonte aberta confiável de "papers premiados".
 */
@Entity
@Table(name = "papers")
@Getter
@Setter
@NoArgsConstructor
public class Paper {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false, length = 1000, unique = true)
    private String url;

    @Column(nullable = false, length = 50)
    private String source = "semanticscholar";

    @Column(length = 300)
    private String venue;

    @Column(name = "publication_year")
    private Integer publicationYear;

    @Column(name = "citation_count")
    private Integer citationCount;

    @Column(length = 500)
    private String authors;

    @Column(name = "cached_at", nullable = false)
    private Instant cachedAt = Instant.now();
}
