package com.appleetcode.backend.src.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "problems")
@Getter
@Setter
@NoArgsConstructor
public class Problem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_slug", nullable = false, length = 200, unique = true)
    private String externalSlug;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(name = "leetcode_url", nullable = false, length = 500)
    private String leetcodeUrl;

    @Column(name = "difficulty_label", nullable = false, length = 20)
    private String difficultyLabel;

    @Column(name = "difficulty_rating")
    private Double difficultyRating;

    @Column(name = "is_core", nullable = false)
    private boolean core;
}
