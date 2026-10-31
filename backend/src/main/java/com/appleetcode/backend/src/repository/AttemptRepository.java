package com.appleetcode.backend.src.repository;

import com.appleetcode.backend.src.entity.Attempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface AttemptRepository extends JpaRepository<Attempt, Long> {

    List<Attempt> findByProblemIdOrderByAttemptedAtDesc(Long problemId);

    List<Attempt> findAllByOrderByAttemptedAtAsc();

    boolean existsByProblemId(Long problemId);

    /** Histórico completo (GET /attempts), mais recente primeiro, com o problema já carregado. */
    @Query("select a from Attempt a join fetch a.problem order by a.attemptedAt desc")
    List<Attempt> findAllWithProblemOrderByAttemptedAtDesc();

    /** Tentativas dentro de um intervalo (ex.: o dia de hoje segundo AppClock), mais recente primeiro. */
    @Query("""
            select a from Attempt a
            join fetch a.problem
            where a.attemptedAt >= :start and a.attemptedAt < :end
            order by a.attemptedAt desc
            """)
    List<Attempt> findByAttemptedAtRangeWithProblem(@Param("start") Instant start, @Param("end") Instant end);
}
