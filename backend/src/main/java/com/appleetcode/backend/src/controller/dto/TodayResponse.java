package com.appleetcode.backend.src.controller.dto;

import com.appleetcode.backend.src.service.SessionSlot;
import com.appleetcode.backend.src.service.TodaySession;

import java.util.List;

/**
 * @param slots      os 2 slots fixos do dia (sequência de aprendizado ou repetição pura -- docs/spec/original-spec.md seção 5)
 * @param reviewBar  sugestões extras de revisão, opcionais, sempre que houver algo pra revisar
 * @param solvedToday tudo que já foi resolvido hoje, venha da sessão, da barra de revisão ou do modo exploração
 */
public record TodayResponse(
        int dayNumber,
        String phase,
        String dayType,
        List<SessionSlotDto> slots,
        List<SessionSlotDto> reviewBar,
        List<AttemptHistoryItemDto> solvedToday
) {
    public static TodayResponse from(
            TodaySession session, List<SessionSlot> reviewBar, List<AttemptHistoryItemDto> solvedToday
    ) {
        List<SessionSlotDto> slots = session.slots().stream().map(SessionSlotDto::from).toList();
        List<SessionSlotDto> reviewBarDtos = reviewBar.stream().map(SessionSlotDto::from).toList();
        return new TodayResponse(
                session.dayNumber(), session.phase().toStateValue(), session.dayType(), slots, reviewBarDtos, solvedToday
        );
    }
}
