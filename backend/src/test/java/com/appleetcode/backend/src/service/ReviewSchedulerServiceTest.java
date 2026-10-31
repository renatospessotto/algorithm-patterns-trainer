package com.appleetcode.backend.src.service;

import com.appleetcode.backend.config.SessionProperties;
import com.appleetcode.backend.src.entity.AttemptResult;
import com.appleetcode.backend.src.entity.Problem;
import com.appleetcode.backend.src.entity.ReviewState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ReviewSchedulerServiceTest {

    private ReviewSchedulerService service;

    @BeforeEach
    void setUp() {
        SessionProperties props = new SessionProperties();
        props.setDifficultyTimeTargetSeconds(Map.of("easy", 720, "medium", 1320, "hard", 2280));
        props.setDifficultyStabilityCapDays(Map.of("easy", 90, "medium", 45, "hard", 20));
        props.setPureRepetitionGapByWeek(Map.of("1", 1, "default", 4));
        service = new ReviewSchedulerService(props);
    }

    private Problem problem(String difficulty) {
        Problem p = new Problem();
        p.setId(1L);
        p.setExternalSlug("two-sum");
        p.setTitle("Two Sum");
        p.setLeetcodeUrl("https://leetcode.com/problems/two-sum/");
        p.setDifficultyLabel(difficulty);
        return p;
    }

    // --- Curva de urgência ---------------------------------------------------

    @Test
    void urgencyIsZeroRightAfterBeingSeen() {
        assertThat(service.calculateUrgency(10.0, 0.0)).isEqualTo(0.0);
    }

    @Test
    void urgencyApproachesOneAsTimePasses() {
        double urgency = service.calculateUrgency(5.0, 500.0);
        assertThat(urgency).isCloseTo(1.0, within(1e-6));
    }

    @Test
    void urgencyNeverExceedsOne() {
        // Pra deltas de tempo extremos (dias/stability > ~745), exp(-x) sofre
        // underflow pra 0.0 em double e a urgência bate exatamente 1.0 -- e
        // isso é esperado, o que a spec garante é "nunca passa de 1", não
        // "estritamente menor que 1" pra qualquer entrada matematicamente
        // possível.
        for (double days = 0; days <= 10_000; days += 137) {
            double urgency = service.calculateUrgency(1.0, days);
            assertThat(urgency).isLessThanOrEqualTo(1.0);
        }
    }

    @Test
    void urgencyIsStrictlyLessThanOneForRealisticTimeframes() {
        // stability_days=1 satura em 1.0 (double) já depois de poucas semanas sem
        // revisão -- o que é aceitável, é só a primeira exposição e o problema
        // sai dessa faixa já na segunda tentativa. Pra estabilidades mais
        // realistas (típicas após 1-2 revisões bem-sucedidas) a urgência segue
        // estritamente abaixo de 1 dentro de um horizonte de 1 ano.
        for (double stabilityDays : new double[] {15.0, 45.0, 90.0}) {
            for (double days = 0; days <= 365; days += 7) {
                double urgency = service.calculateUrgency(stabilityDays, days);
                assertThat(urgency).isLessThan(1.0);
            }
        }
    }

    @Test
    void urgencyEqualsExpectedClassicForgettingCurveValue() {
        // urgencia = 1 - exp(-dias/stability); dias == stability -> 1 - e^-1 ≈ 0.6321
        double urgency = service.calculateUrgency(7.0, 7.0);
        assertThat(urgency).isCloseTo(1 - Math.exp(-1), within(1e-9));
    }

    @Test
    void higherStabilityMeansLowerUrgencyForSameElapsedTime() {
        double urgencyFragile = service.calculateUrgency(2.0, 10.0);
        double urgencyStable = service.calculateUrgency(30.0, 10.0);
        assertThat(urgencyFragile).isGreaterThan(urgencyStable);
    }

    // --- Primeira exposição ---------------------------------------------------

    @Test
    void firstExposureAlwaysStartsWithStabilityOfOneDayRegardlessOfResult() {
        Instant now = Instant.now();
        ReviewState state = service.recordAttempt(null, problem("Medium"), AttemptResult.SOLVED_FAST_NAMED_PATTERN, 600, now);

        assertThat(state.getStabilityDays()).isEqualTo(1.0);
        assertThat(state.getEmaQuality()).isEqualTo(5.0);
        assertThat(state.getAttemptsCount()).isEqualTo(1);
        assertThat(state.getSuccessesCount()).isEqualTo(1);
        assertThat(state.getLastSeenAt()).isEqualTo(now);
    }

    @Test
    void firstExposureWithFailureStillStartsWithStabilityOfOneDay() {
        ReviewState state = service.recordAttempt(null, problem("Medium"), AttemptResult.GAVE_UP, 1000, Instant.now());

        assertThat(state.getStabilityDays()).isEqualTo(1.0);
        assertThat(state.getEmaQuality()).isEqualTo(0.0);
        assertThat(state.getSuccessesCount()).isEqualTo(0);
    }

    // --- Queda pela metade em falha --------------------------------------------

    @Test
    void stabilityHalvesWhenQualityBelowThree() {
        ReviewState existing = existingState(10.0, 4.0, 3, 3);

        ReviewState updated = service.recordAttempt(existing, problem("Medium"), AttemptResult.FAILED, 1300, Instant.now());

        assertThat(updated.getStabilityDays()).isEqualTo(5.0);
    }

    @Test
    void stabilityNeverGoesBelowOneDayAfterHalving() {
        ReviewState existing = existingState(1.5, 4.0, 3, 3);

        ReviewState updated = service.recordAttempt(existing, problem("Medium"), AttemptResult.GAVE_UP, 1300, Instant.now());

        assertThat(updated.getStabilityDays()).isEqualTo(1.0);
    }

    // --- Fatores de crescimento por faixa de ema_quality ------------------------

    @ParameterizedTest
    @CsvSource({
            "3.0, 1.3",
            "3.9, 1.3",
            "4.0, 1.8",
            "4.9, 1.8",
            "5.0, 2.3",
    })
    void growthFactorMatchesEmaQualityBand(double ema, double expectedFactor) {
        assertThat(service.growthFactorForEmaQuality(ema)).isEqualTo(expectedFactor);
    }

    @Test
    void growthFactorDefaultsToNeutralBelowThreeBand() {
        assertThat(service.growthFactorForEmaQuality(2.9)).isEqualTo(1.0);
    }

    // --- Fator de tempo ---------------------------------------------------------

    @Test
    void timeFactorIsBoostedWhenFasterThanOrEqualToTarget() {
        // medium target = 1320s
        assertThat(service.timeFactorFor(1320, "Medium")).isEqualTo(1.15);
        assertThat(service.timeFactorFor(600, "Medium")).isEqualTo(1.15);
    }

    @Test
    void timeFactorIsPenalizedWhenMuchSlowerThanTarget() {
        // 1.5x de 1320 = 1980; precisa ser > 1.5, nao == 1.5
        assertThat(service.timeFactorFor(1981, "Medium")).isEqualTo(0.75);
    }

    @Test
    void timeFactorIsNeutralInBetween() {
        assertThat(service.timeFactorFor(1500, "Medium")).isEqualTo(1.0);
    }

    // --- Teto de stability_days por dificuldade ---------------------------------

    @Test
    void stabilityIsCappedAtNinetyDaysForEasyProblems() {
        ReviewState existing = existingState(80.0, 5.0, 5, 5);

        ReviewState updated = service.recordAttempt(existing, problem("Easy"), AttemptResult.SOLVED_FAST_NAMED_PATTERN, 100, Instant.now());

        assertThat(updated.getStabilityDays()).isEqualTo(90.0);
    }

    @Test
    void stabilityIsCappedAtFortyFiveDaysForMediumProblems() {
        ReviewState existing = existingState(40.0, 5.0, 5, 5);

        ReviewState updated = service.recordAttempt(existing, problem("Medium"), AttemptResult.SOLVED_FAST_NAMED_PATTERN, 100, Instant.now());

        assertThat(updated.getStabilityDays()).isEqualTo(45.0);
    }

    @Test
    void stabilityIsCappedAtTwentyDaysForHardProblems() {
        ReviewState existing = existingState(18.0, 5.0, 5, 5);

        ReviewState updated = service.recordAttempt(existing, problem("Hard"), AttemptResult.SOLVED_FAST_NAMED_PATTERN, 100, Instant.now());

        assertThat(updated.getStabilityDays()).isEqualTo(20.0);
    }

    @Test
    void growthDoesNotHitCapWhenFarBelowIt() {
        // ema alto (5) mas stability baixo (2 dias): 2 * 2.3 * 1.15 = 5.29, bem abaixo do teto de 45 (medium)
        ReviewState existing = existingState(2.0, 5.0, 2, 2);

        ReviewState updated = service.recordAttempt(existing, problem("Medium"), AttemptResult.SOLVED_FAST_NAMED_PATTERN, 100, Instant.now());

        assertThat(updated.getStabilityDays()).isCloseTo(2.0 * 2.3 * 1.15, within(1e-9));
    }

    // --- EMA de qualidade ---------------------------------------------------

    @Test
    void emaQualityIsWeightedTowardsHistoryButReactsToRecentResult() {
        ReviewState existing = existingState(5.0, 4.0, 3, 3);

        ReviewState updated = service.recordAttempt(existing, problem("Medium"), AttemptResult.GAVE_UP, 100, Instant.now());

        // 0.7*4 + 0.3*0 = 2.8
        assertThat(updated.getEmaQuality()).isCloseTo(2.8, within(1e-9));
    }

    @Test
    void aSingleBadDayDoesNotTankEmaQualityCompletely() {
        ReviewState existing = existingState(5.0, 4.5, 10, 9);

        ReviewState updated = service.recordAttempt(existing, problem("Medium"), AttemptResult.FAILED, 100, Instant.now());

        // 0.7*4.5 + 0.3*1 = 3.45 -- cai, mas continua bem acima de zero
        assertThat(updated.getEmaQuality()).isCloseTo(3.45, within(1e-9));
    }

    // --- Contadores ---------------------------------------------------------

    @Test
    void attemptsCountAlwaysIncrementsButSuccessesOnlyOnQGreaterOrEqualThree() {
        ReviewState existing = existingState(5.0, 4.0, 3, 2);

        ReviewState updated = service.recordAttempt(existing, problem("Medium"), AttemptResult.SOLVED_WITH_HINT, 100, Instant.now());

        assertThat(updated.getAttemptsCount()).isEqualTo(4);
        assertThat(updated.getSuccessesCount()).isEqualTo(3);
    }

    @Test
    void lastSeenAtIsAlwaysUpdatedToNow() {
        ReviewState existing = existingState(5.0, 4.0, 3, 2);
        existing.setLastSeenAt(Instant.now().minus(30, ChronoUnit.DAYS));
        Instant now = Instant.now();

        ReviewState updated = service.recordAttempt(existing, problem("Medium"), AttemptResult.SOLVED, 100, now);

        assertThat(updated.getLastSeenAt()).isEqualTo(now);
    }

    private ReviewState existingState(double stabilityDays, double emaQuality, int attemptsCount, int successesCount) {
        ReviewState state = new ReviewState();
        state.setProblemId(1L);
        state.setStabilityDays(stabilityDays);
        state.setEmaQuality(emaQuality);
        state.setLastSeenAt(Instant.now().minus(5, ChronoUnit.DAYS));
        state.setAttemptsCount(attemptsCount);
        state.setSuccessesCount(successesCount);
        return state;
    }
}
