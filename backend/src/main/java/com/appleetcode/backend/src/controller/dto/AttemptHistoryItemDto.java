package com.appleetcode.backend.src.controller.dto;

import com.appleetcode.backend.src.entity.Attempt;
import com.appleetcode.backend.src.entity.AttemptResult;
import com.appleetcode.backend.src.entity.Problem;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Um item do histórico de tentativas (GET /attempts), pra alimentar a aba
 * "Histórico" pedida pelo usuário -- precisa ficar bem acessível quais
 * exercícios já foram feitos e as observações/notas registradas em cada um.
 *
 * @param attemptedAtLocalDate dia (yyyy-MM-dd) segundo o fuso do app (AppClock)
 *        -- calculado no backend pra bater exatamente com os dias do
 *        heatmap de /stats. Filtrar no frontend fatiando a string ISO de
 *        `attemptedAt` (UTC) dava dia errado pra tentativas feitas à noite.
 */
public record AttemptHistoryItemDto(
        Long attemptId,
        Long problemId,
        String problemTitle,
        String problemSlug,
        String leetcodeUrl,
        String difficultyLabel,
        Instant attemptedAt,
        String attemptedAtLocalDate,
        Integer durationSeconds,
        AttemptResult result,
        Short confidence,
        String note
) {
    public static AttemptHistoryItemDto from(Attempt attempt, ZoneId zone) {
        Problem problem = attempt.getProblem();
        LocalDate localDate = attempt.getAttemptedAt().atZone(zone).toLocalDate();
        return new AttemptHistoryItemDto(
                attempt.getId(),
                problem.getId(),
                problem.getTitle(),
                problem.getExternalSlug(),
                problem.getLeetcodeUrl(),
                problem.getDifficultyLabel(),
                attempt.getAttemptedAt(),
                localDate.toString(),
                attempt.getDurationSeconds(),
                attempt.getResult(),
                attempt.getConfidence(),
                attempt.getNote()
        );
    }
}
