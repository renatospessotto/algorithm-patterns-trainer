package com.appleetcode.backend.src.repository;

import com.appleetcode.backend.src.entity.Problem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProblemRepository extends JpaRepository<Problem, Long> {

    Optional<Problem> findByExternalSlug(String externalSlug);

    long countByCore(boolean isCore);

    @Query("""
            select p from Problem p
            join ProblemPattern pp on pp.problem = p
            where pp.pattern.slug = :patternSlug
              and p.core = false
              and p.id not in (select a.problem.id from Attempt a)
            """)
    List<Problem> findUnattemptedPoolByPattern(@Param("patternSlug") String patternSlug);

    @Query("""
            select p from Problem p
            join ProblemPattern pp on pp.problem = p
            where pp.pattern.slug = :patternSlug
              and p.core = false
            """)
    List<Problem> findPoolByPattern(@Param("patternSlug") String patternSlug);

    @Query("select p from Problem p where p.core = false and p.id not in (select a.problem.id from Attempt a)")
    List<Problem> findUnattemptedPool();

    @Query("select p from Problem p where p.core = false")
    List<Problem> findPool();
}
