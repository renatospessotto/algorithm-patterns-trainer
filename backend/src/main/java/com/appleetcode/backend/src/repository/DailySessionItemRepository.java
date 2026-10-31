package com.appleetcode.backend.src.repository;

import com.appleetcode.backend.src.entity.DailySessionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DailySessionItemRepository extends JpaRepository<DailySessionItem, Long> {

    @Query("""
            select d from DailySessionItem d
            join fetch d.problem
            where d.dayNumber = :dayNumber
            order by d.slotIndex asc
            """)
    List<DailySessionItem> findByDayNumberOrderBySlotIndexAsc(int dayNumber);
}
