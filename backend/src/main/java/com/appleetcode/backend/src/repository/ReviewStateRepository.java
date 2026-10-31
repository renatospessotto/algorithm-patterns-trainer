package com.appleetcode.backend.src.repository;

import com.appleetcode.backend.src.entity.ReviewState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ReviewStateRepository extends JpaRepository<ReviewState, Long> {

    @Query("select rs from ReviewState rs join fetch rs.problem")
    List<ReviewState> findAllWithProblem();
}
