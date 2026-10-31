package com.appleetcode.backend.src.repository;

import com.appleetcode.backend.src.entity.Paper;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaperRepository extends JpaRepository<Paper, Long> {

    List<Paper> findByTopicIdOrderByCitationCountDescCachedAtDesc(Long topicId);

    List<Paper> findAllByOrderByCitationCountDescCachedAtDesc();

    Optional<Paper> findByUrl(String url);
}
