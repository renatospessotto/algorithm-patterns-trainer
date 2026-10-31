package com.appleetcode.backend.src.service;

import com.appleetcode.backend.src.entity.Phase;

import java.util.List;

public record TodaySession(
        int dayNumber,
        Phase phase,
        String dayType, // "aprendizado" ou "repeticao_pura"
        List<SessionSlot> slots
) {
}
