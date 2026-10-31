package com.appleetcode.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * Constantes do algoritmo de fases/repetição (docs/spec/original-spec.md seções 4 e 5), lidas
 * de application.yml (app.session.*) em vez de hardcoded — a ideia da spec é
 * que dá pra ajustar essa curva depois de usar o app umas semanas sem
 * recompilar código.
 */
@ConfigurationProperties(prefix = "app.session")
public class SessionProperties {

    /** difficulty_label (easy/medium/hard, lowercase) -> tempo alvo em segundos (12/22/38 min). */
    private Map<String, Integer> difficultyTimeTargetSeconds;

    /** difficulty_label (easy/medium/hard, lowercase) -> teto de stability_days (90/45/20). */
    private Map<String, Integer> difficultyStabilityCapDays;

    /** número da semana (1-indexed, string) -> máximo de dias de aprendizado seguidos; "default" cobre semana 7+. */
    private Map<String, Integer> pureRepetitionGapByWeek;

    public Map<String, Integer> getDifficultyTimeTargetSeconds() {
        return difficultyTimeTargetSeconds;
    }

    public void setDifficultyTimeTargetSeconds(Map<String, Integer> difficultyTimeTargetSeconds) {
        this.difficultyTimeTargetSeconds = difficultyTimeTargetSeconds;
    }

    public Map<String, Integer> getDifficultyStabilityCapDays() {
        return difficultyStabilityCapDays;
    }

    public void setDifficultyStabilityCapDays(Map<String, Integer> difficultyStabilityCapDays) {
        this.difficultyStabilityCapDays = difficultyStabilityCapDays;
    }

    public Map<String, Integer> getPureRepetitionGapByWeek() {
        return pureRepetitionGapByWeek;
    }

    public void setPureRepetitionGapByWeek(Map<String, Integer> pureRepetitionGapByWeek) {
        this.pureRepetitionGapByWeek = pureRepetitionGapByWeek;
    }

    public int timeTargetSecondsFor(String difficultyLabel) {
        return difficultyTimeTargetSeconds.get(difficultyLabel.toLowerCase());
    }

    public int stabilityCapDaysFor(String difficultyLabel) {
        return difficultyStabilityCapDays.get(difficultyLabel.toLowerCase());
    }

    /** Limite de dias de aprendizado seguidos pra semana atual (1-indexed a partir do current_day). */
    public int pureRepetitionGapForWeek(int weekNumber) {
        Integer specific = pureRepetitionGapByWeek.get(String.valueOf(weekNumber));
        if (specific != null) {
            return specific;
        }
        return pureRepetitionGapByWeek.get("default");
    }
}
