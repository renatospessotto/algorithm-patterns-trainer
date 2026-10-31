package com.appleetcode.backend.src.controller.dto;

import com.appleetcode.backend.src.service.SessionSlot;

public record SessionSlotDto(ProblemDto problem, String origin, Double urgency) {
    public static SessionSlotDto from(SessionSlot slot) {
        return new SessionSlotDto(ProblemDto.from(slot.problem()), slot.origin(), slot.urgency());
    }
}
