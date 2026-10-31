package com.appleetcode.backend.src.repository;

import com.appleetcode.backend.src.entity.ProblemPattern;
import com.appleetcode.backend.src.entity.ProblemPatternId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProblemPatternRepository extends JpaRepository<ProblemPattern, ProblemPatternId> {

    @Query("select pp from ProblemPattern pp where pp.pattern.id = :patternId")
    List<ProblemPattern> findByPatternId(@Param("patternId") Long patternId);

    @Query("select pp from ProblemPattern pp where pp.problem.id = :problemId")
    List<ProblemPattern> findByProblemId(@Param("problemId") Long problemId);

    /**
     * Padrões ordenados por ema_quality média (dos problemas já tentados que
     * cobrem aquele padrão), do mais fraco pro mais forte. Usado na fase de
     * consolidação pra escolher o "ponto fraco" (docs/spec/original-spec.md seção 5).
     */
    @Query("""
            select pp.pattern.id as patternId, avg(rs.emaQuality) as avgEmaQuality
            from ProblemPattern pp join ReviewState rs on rs.problemId = pp.problem.id
            group by pp.pattern.id
            order by avg(rs.emaQuality) asc
            """)
    List<PatternEmaProjection> findPatternsOrderedByAvgEmaQualityAsc();

    interface PatternEmaProjection {
        Long getPatternId();
        Double getAvgEmaQuality();
    }

    @Query("select count(pp) from ProblemPattern pp where pp.pattern.id = :patternId and pp.problem.core = true")
    long countCoreByPattern(@Param("patternId") Long patternId);

    @Query("""
            select count(pp) from ProblemPattern pp
            join ReviewState rs on rs.problemId = pp.problem.id
            where pp.pattern.id = :patternId and pp.problem.core = true and rs.successesCount > 0
            """)
    long countCoreSolvedByPattern(@Param("patternId") Long patternId);

    @Query("""
            select avg(rs.emaQuality) from ProblemPattern pp
            join ReviewState rs on rs.problemId = pp.problem.id
            where pp.pattern.id = :patternId
            """)
    Double avgEmaQualityByPattern(@Param("patternId") Long patternId);
}
