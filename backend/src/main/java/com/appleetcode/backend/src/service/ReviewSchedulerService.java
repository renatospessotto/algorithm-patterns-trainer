package com.appleetcode.backend.src.service;

import com.appleetcode.backend.config.SessionProperties;
import com.appleetcode.backend.src.entity.AttemptResult;
import com.appleetcode.backend.src.entity.Problem;
import com.appleetcode.backend.src.entity.ReviewState;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/**
 * Fila de prioridade contínua (docs/spec/original-spec.md seção 4). Isolado de qualquer
 * acesso a repositório pra ficar 100% testável com JUnit puro: recebe e
 * devolve entidades já carregadas/atualizadas, quem persiste é o chamador.
 */
@Service
public class ReviewSchedulerService {

    /** Piso de stability_days: nunca deixa cair pra zero (dividiria por zero na urgência). */
    private static final double MIN_STABILITY_DAYS = 1.0;
    private static final double FIRST_EXPOSURE_STABILITY_DAYS = 1.0;
    private static final double FAILURE_THRESHOLD_Q = 3.0;
    private static final double EMA_ALPHA_NEW = 0.3;
    private static final double EMA_ALPHA_OLD = 0.7;

    private final SessionProperties sessionProperties;

    public ReviewSchedulerService(SessionProperties sessionProperties) {
        this.sessionProperties = sessionProperties;
    }

    /**
     * urgência = 1 - exp(-dias_desde_ultima_vez / stability_days), sempre em [0, 1).
     */
    public double calculateUrgency(double stabilityDays, double daysSinceLastSeen) {
        double safeStability = Math.max(stabilityDays, MIN_STABILITY_DAYS);
        double safeDays = Math.max(daysSinceLastSeen, 0.0);
        return 1.0 - Math.exp(-safeDays / safeStability);
    }

    public double calculateUrgency(ReviewState state, Instant now) {
        double daysSinceLastSeen = Duration.between(state.getLastSeenAt(), now).toMinutes() / (24.0 * 60.0);
        return calculateUrgency(state.getStabilityDays(), daysSinceLastSeen);
    }

    /**
     * Aplica o resultado de uma tentativa a um review_state (cria um novo se
     * `existing` for null — primeira exposição ao problema) e devolve o estado
     * atualizado, pronto pra ser salvo pelo chamador.
     */
    public ReviewState recordAttempt(
            ReviewState existing,
            Problem problem,
            AttemptResult result,
            int durationSeconds,
            Instant now
    ) {
        boolean firstExposure = existing == null;
        ReviewState state = firstExposure ? new ReviewState(problem) : existing;
        int q = result.qualityScore();

        double newEmaQuality = firstExposure
                ? q
                : EMA_ALPHA_OLD * state.getEmaQuality() + EMA_ALPHA_NEW * q;
        state.setEmaQuality(newEmaQuality);

        double newStabilityDays;
        if (firstExposure) {
            newStabilityDays = FIRST_EXPOSURE_STABILITY_DAYS;
        } else if (q < FAILURE_THRESHOLD_Q) {
            newStabilityDays = Math.max(MIN_STABILITY_DAYS, state.getStabilityDays() / 2.0);
        } else {
            double growthFactor = growthFactorForEmaQuality(newEmaQuality);
            double timeFactor = timeFactorFor(durationSeconds, problem.getDifficultyLabel());
            double cap = sessionProperties.stabilityCapDaysFor(problem.getDifficultyLabel());
            newStabilityDays = Math.min(cap, state.getStabilityDays() * growthFactor * timeFactor);
        }
        state.setStabilityDays(newStabilityDays);

        state.setLastSeenAt(now);
        state.setAttemptsCount(state.getAttemptsCount() + 1);
        if (q >= FAILURE_THRESHOLD_Q) {
            state.setSuccessesCount(state.getSuccessesCount() + 1);
        }
        return state;
    }

    /**
     * Fator base de crescimento por faixa de ema_quality (seção 4):
     * [3, 4) -> 1.3, [4, 5) -> 1.8, 5 -> 2.3. Abaixo de 3 (só possível quando
     * q pontual >= 3 mas o histórico geral ainda está ruim) não há regra
     * definida na spec; por segurança não cresce nem encolhe (fator 1.0).
     */
    double growthFactorForEmaQuality(double emaQuality) {
        if (emaQuality >= 5.0) {
            return 2.3;
        } else if (emaQuality >= 4.0) {
            return 1.8;
        } else if (emaQuality >= 3.0) {
            return 1.3;
        }
        return 1.0;
    }

    /**
     * Fator de tempo (seção 4): time_ratio <= 1 -> 1.15, time_ratio > 1.5 -> 0.75,
     * intermediário -> 1.0 (sem alteração).
     */
    double timeFactorFor(int durationSeconds, String difficultyLabel) {
        int targetSeconds = sessionProperties.timeTargetSecondsFor(difficultyLabel);
        double timeRatio = durationSeconds / (double) targetSeconds;
        if (timeRatio <= 1.0) {
            return 1.15;
        } else if (timeRatio > 1.5) {
            return 0.75;
        }
        return 1.0;
    }
}
