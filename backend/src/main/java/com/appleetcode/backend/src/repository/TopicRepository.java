package com.appleetcode.backend.src.repository;

import com.appleetcode.backend.src.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TopicRepository extends JpaRepository<Topic, Long> {
    Optional<Topic> findByNameIgnoreCase(String name);
}
