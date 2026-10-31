package com.appleetcode.backend.src.repository;

import com.appleetcode.backend.src.entity.PlanItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PlanItemRepository extends JpaRepository<PlanItem, Long> {

    List<PlanItem> findAllByOrderByDayNumberAsc();

    /**
     * Próximo problema novo a introduzir: o item da sequência fixa, na ordem de
     * day_number, cujo problema ainda não tem review_state (ou seja, nunca foi
     * tentado). Não depende de um contador separado em app_state — "introduzido"
     * é sinônimo de "já tem review_state".
     */
    @Query("""
            select pi from PlanItem pi
            join fetch pi.problem
            join fetch pi.pattern
            where pi.problem.id not in (select rs.problemId from ReviewState rs)
            order by pi.dayNumber asc
            """)
    List<PlanItem> findNextNotIntroduced();

    long count();
}
