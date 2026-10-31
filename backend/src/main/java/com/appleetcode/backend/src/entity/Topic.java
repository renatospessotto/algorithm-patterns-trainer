package com.appleetcode.backend.src.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Tema de leitura (banco de dados, IA, ML tradicional etc), com uma query de busca de papers. */
@Entity
@Table(name = "topics")
@Getter
@Setter
@NoArgsConstructor
public class Topic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "search_query", nullable = false, length = 500)
    private String searchQuery;

    public Topic(String name, String searchQuery) {
        this.name = name;
        this.searchQuery = searchQuery;
    }
}
