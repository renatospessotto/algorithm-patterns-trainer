package com.appleetcode.backend.src.repository;

import com.appleetcode.backend.src.entity.Pattern;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PatternRepository extends JpaRepository<Pattern, Long> {
    Optional<Pattern> findBySlug(String slug);
}
