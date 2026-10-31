package com.appleetcode.backend.src.service;

import com.appleetcode.backend.config.AppClock;
import com.appleetcode.backend.config.SessionProperties;
import com.appleetcode.backend.src.entity.AppState;
import com.appleetcode.backend.src.entity.Pattern;
import com.appleetcode.backend.src.entity.PlanItem;
import com.appleetcode.backend.src.entity.Problem;
import com.appleetcode.backend.src.repository.AppStateRepository;
import com.appleetcode.backend.src.repository.DailySessionItemRepository;
import com.appleetcode.backend.src.repository.PatternRepository;
import com.appleetcode.backend.src.repository.PlanItemRepository;
import com.appleetcode.backend.src.repository.ProblemPatternRepository;
import com.appleetcode.backend.src.repository.AttemptRepository;
import com.appleetcode.backend.src.repository.ProblemRepository;
import com.appleetcode.backend.src.service.ReviewQueueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SessionPlannerServiceTest {

    private AppStateRepository appStateRepository;
    private PlanItemRepository planItemRepository;
    private DailySessionItemRepository dailySessionItemRepository;
    private ReviewQueueService reviewQueueService;
    private SessionPlannerService service;

    @BeforeEach
    void setUp() {
        appStateRepository = mock(AppStateRepository.class);
        planItemRepository = mock(PlanItemRepository.class);
        dailySessionItemRepository = mock(DailySessionItemRepository.class);
        PatternRepository patternRepository = mock(PatternRepository.class);
        ProblemRepository problemRepository = mock(ProblemRepository.class);
        ProblemPatternRepository problemPatternRepository = mock(ProblemPatternRepository.class);
        reviewQueueService = mock(ReviewQueueService.class);
        AppClock appClock = mock(AppClock.class);
        when(appClock.today()).thenReturn(LocalDate.now());

        SessionProperties props = new SessionProperties();
        props.setDifficultyTimeTargetSeconds(Map.of("easy", 720, "medium", 1320, "hard", 2280));
        props.setDifficultyStabilityCapDays(Map.of("easy", 90, "medium", 45, "hard", 20));
        props.setPureRepetitionGapByWeek(Map.of("1", 1, "default", 4));

        service = new SessionPlannerService(
                appStateRepository, planItemRepository, patternRepository,
                problemRepository, problemPatternRepository, dailySessionItemRepository,
                reviewQueueService, props, appClock, mock(AttemptRepository.class)
        );

        // Toda chamada de save() em app_state só devolve o próprio argumento (evita NPE de mock).
        when(appStateRepository.save(any(AppState.class))).thenAnswer(inv -> inv.getArgument(0));
        // Nenhum dia tem sessão fixada ainda por padrão (testes cobrem a primeira montagem do dia).
        when(dailySessionItemRepository.findByDayNumberOrderBySlotIndexAsc(org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(List.of());
    }

    private void stubFreshAppState() {
        when(appStateRepository.findById("last_session_date")).thenReturn(Optional.empty());
        when(appStateRepository.findById(AppState.CURRENT_DAY)).thenReturn(Optional.of(new AppState(AppState.CURRENT_DAY, "1")));
        when(appStateRepository.findById(AppState.CURRENT_PHASE)).thenReturn(Optional.of(new AppState(AppState.CURRENT_PHASE, "introducao")));
        when(appStateRepository.findById(AppState.LAST_PURE_REPETITION_DAY)).thenReturn(Optional.of(new AppState(AppState.LAST_PURE_REPETITION_DAY, "0")));
    }

    private PlanItem planItem(int dayNumber) {
        Problem problem = new Problem();
        problem.setId(1L);
        problem.setExternalSlug("two-sum");
        problem.setTitle("Two Sum");
        problem.setLeetcodeUrl("https://leetcode.com/problems/two-sum/");
        problem.setDifficultyLabel("Easy");

        Pattern pattern = new Pattern();
        pattern.setId(1L);
        pattern.setSlug("hashmap-set");
        pattern.setOrderInPlan(1);

        PlanItem item = new PlanItem();
        item.setDayNumber(dayNumber);
        item.setProblem(problem);
        item.setPattern(pattern);
        item.setSlot("novo");
        return item;
    }

    /**
     * Regressão: no primeiro dia de uso a sessão precisa ser de aprendizado,
     * com pelo menos um problema novo -- não pode cair em "repeticao_pura"
     * com a fila vazia (sessão em branco).
     */
    @Test
    void firstDayEverIsALearningDayWithAtLeastOneNewProblem() {
        stubFreshAppState();
        when(planItemRepository.findNextNotIntroduced()).thenReturn(List.of(planItem(1)));
        when(reviewQueueService.currentQueue()).thenReturn(List.of());

        TodaySession session = service.buildTodaySession();

        assertThat(session.dayType()).isEqualTo("aprendizado");
        assertThat(session.slots()).isNotEmpty();
        assertThat(session.slots().get(0).origin()).isEqualTo("novo");
    }

    /**
     * Regressão do bug reportado pelo usuário: resolver dois problemas novos
     * no dia 1 (ex. Two Sum e Group Anagrams) não pode fazer esses dois
     * problemas voltarem como "revisão" já no dia 2. Enquanto o ciclo básico
     * (sequência fixa de plan_items) não terminar, todo dia é de aprendizado
     * e os dois slots vêm só de problemas ainda não introduzidos -- a caixa
     * de revisão fica reservada pra depois que o ciclo básico acabar.
     */
    @Test
    void secondDayStaysInLearningModeWithNewProblemsWhileBasicCycleIsNotFinished() {
        when(appStateRepository.findById("last_session_date"))
                .thenReturn(Optional.of(new AppState("last_session_date", java.time.LocalDate.now().minusDays(1).toString())));
        when(appStateRepository.findById(AppState.CURRENT_DAY)).thenReturn(Optional.of(new AppState(AppState.CURRENT_DAY, "1")));
        when(appStateRepository.findById(AppState.CURRENT_PHASE)).thenReturn(Optional.of(new AppState(AppState.CURRENT_PHASE, "introducao")));
        when(appStateRepository.findById(AppState.LAST_PURE_REPETITION_DAY)).thenReturn(Optional.of(new AppState(AppState.LAST_PURE_REPETITION_DAY, "0")));
        when(planItemRepository.findNextNotIntroduced()).thenReturn(List.of(planItem(3), planItem(4)));
        // Fila de revisão já tem os problemas do dia 1 (Two Sum, Group Anagrams),
        // mas isso não pode fazer diferença nenhuma enquanto o ciclo básico não terminar.
        when(reviewQueueService.currentQueue()).thenReturn(List.of(
                new com.appleetcode.backend.src.service.UrgentReviewItem(planItem(1).getProblem(), null, 0.63)
        ));

        TodaySession session = service.buildTodaySession();

        assertThat(session.dayNumber()).isEqualTo(2);
        assertThat(session.dayType()).isEqualTo("aprendizado");
        assertThat(session.slots()).allMatch(s -> s.origin().equals("novo"));
    }
}
