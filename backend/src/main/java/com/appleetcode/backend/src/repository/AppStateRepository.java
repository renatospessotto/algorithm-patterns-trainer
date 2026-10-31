package com.appleetcode.backend.src.repository;

import com.appleetcode.backend.src.entity.AppState;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppStateRepository extends JpaRepository<AppState, String> {
}
