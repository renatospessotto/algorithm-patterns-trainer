package com.appleetcode.backend.src.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "app_state")
@Getter
@Setter
@NoArgsConstructor
public class AppState {

    @Id
    @Column(length = 60)
    private String key;

    @Column(nullable = false, length = 200)
    private String value;

    public AppState(String key, String value) {
        this.key = key;
        this.value = value;
    }

    // Chaves conhecidas
    public static final String CURRENT_PHASE = "current_phase";
    public static final String CURRENT_DAY = "current_day";
    public static final String LAST_PURE_REPETITION_DAY = "last_pure_repetition_day";
}
