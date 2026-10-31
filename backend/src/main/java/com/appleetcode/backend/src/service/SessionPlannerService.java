package com.appleetcode.backend.src.service;

import com.appleetcode.backend.config.AppClock;
import com.appleetcode.backend.config.SessionProperties;
import com.appleetcode.backend.src.entity.AppState;
import com.appleetcode.backend.src.entity.DailySessionItem;
import com.appleetcode.backend.src.entity.Pattern;
import com.appleetcode.backend.src.entity.Phase;
import com.appleetcode.backend.src.entity.PlanItem;
import com.appleetcode.backend.src.entity.Problem;
import com.appleetcode.backend.src.controller.dto.AttemptHistoryItemDto;
import com.appleetcode.backend.src.controller.dto.TodayResponse;
import com.appleetcode.backend.src.entity.Attempt;
import com.appleetcode.backend.src.repository.AppStateRepository;
import com.appleetcode.backend.src.repository.AttemptRepository;
import com.appleetcode.backend.src.repository.DailySessionItemRepository;
import com.appleetcode.backend.src.repository.PatternRepository;
import com.appleetcode.backend.src.repository.PlanItemRepository;
import com.appleetcode.backend.src.repository.ProblemPatternRepository;
import com.appleetcode.backend.src.repository.ProblemRepository;
import com.appleetcode.backend.src.service.ReviewQueueService;
import com.appleetcode.backend.src.service.UrgentReviewItem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Fases e fluxo diário (docs/spec/original-spec.md seção 5). Decide, na primeira chamada de
 * GET /today de cada dia, se o dia é de aprendizado ou repetição pura e
 * monta os 2 slots -- e fixa essa decisão em `daily_session_items`, então
 * chamadas seguintes no mesmo dia (e o próprio ato de resolver um dos
 * problemas) não mudam o que está na tela.
 */
@Service
public class SessionPlannerService {

    /** Piso de urgência abaixo do qual um item não é considerado "relevante" pra completar slot na consolidação. */
    private static final double CONSOLIDATION_URGENCY_FLOOR = 0.15;
    /** Quantos itens extras a "barra de revisão opcional" sugere, no máximo. */
    private static final int REVIEW_BAR_SIZE = 3;
    private static final String LAST_SESSION_DATE_KEY = "last_session_date";

    private final AppStateRepository appStateRepository;
    private final PlanItemRepository planItemRepository;
    private final PatternRepository patternRepository;
    private final ProblemRepository problemRepository;
    private final ProblemPatternRepository problemPatternRepository;
    private final DailySessionItemRepository dailySessionItemRepository;
    private final ReviewQueueService reviewQueueService;
    private final SessionProperties sessionProperties;
    private final AppClock appClock;
    private final AttemptRepository attemptRepository;
    private final Random random = new Random();

    public SessionPlannerService(
            AppStateRepository appStateRepository,
            PlanItemRepository planItemRepository,
            PatternRepository patternRepository,
            ProblemRepository problemRepository,
            ProblemPatternRepository problemPatternRepository,
            DailySessionItemRepository dailySessionItemRepository,
            ReviewQueueService reviewQueueService,
            SessionProperties sessionProperties,
            AppClock appClock,
            AttemptRepository attemptRepository
    ) {
        this.appStateRepository = appStateRepository;
        this.planItemRepository = planItemRepository;
        this.patternRepository = patternRepository;
        this.problemRepository = problemRepository;
        this.problemPatternRepository = problemPatternRepository;
        this.dailySessionItemRepository = dailySessionItemRepository;
        this.reviewQueueService = reviewQueueService;
        this.sessionProperties = sessionProperties;
        this.appClock = appClock;
        this.attemptRepository = attemptRepository;
    }

    /**
     * Monta a resposta completa de GET /today: sessão do dia + resolvidos
     * hoje (venham da sessão principal, da barra de revisão ou do modo
     * exploração, já que todos são tentativas na mesma tabela `attempts`) +
     * barra de revisão opcional.
     */
    @Transactional
    public TodayResponse today() {
        TodaySession session = buildTodaySession();

        Instant start = appClock.startOfToday();
        Instant end = appClock.startOfTomorrow();
        List<Attempt> solvedTodayAttempts = attemptRepository.findByAttemptedAtRangeWithProblem(start, end);
        Set<Long> solvedTodayProblemIds = solvedTodayAttempts.stream()
                .map(a -> a.getProblem().getId())
                .collect(Collectors.toSet());

        List<SessionSlot> reviewBar = reviewBar(session, solvedTodayProblemIds);
        List<AttemptHistoryItemDto> solvedToday = solvedTodayAttempts.stream()
                .map(a -> AttemptHistoryItemDto.from(a, appClock.zone()))
                .toList();

        return TodayResponse.from(session, reviewBar, solvedToday);
    }

    @Transactional
    public TodaySession buildTodaySession() {
        advanceCurrentDayIfNewCalendarDay();

        int currentDay = readInt(AppState.CURRENT_DAY);
        Phase phase = Phase.fromStateValue(readString(AppState.CURRENT_PHASE));

        List<DailySessionItem> persisted = dailySessionItemRepository.findByDayNumberOrderBySlotIndexAsc(currentDay);
        if (!persisted.isEmpty()) {
            List<SessionSlot> slots = persisted.stream()
                    .map(item -> new SessionSlot(item.getProblem(), item.getOrigin(), item.getUrgency()))
                    .toList();
            String dayType = slots.stream().anyMatch(s -> "novo".equals(s.origin())) ? "aprendizado" : "repeticao_pura";
            return new TodaySession(currentDay, phase, dayType, slots);
        }

        List<PlanItem> notIntroduced = planItemRepository.findNextNotIntroduced();
        if (phase == Phase.INTRODUCAO && notIntroduced.isEmpty()) {
            phase = Phase.CONSOLIDACAO;
            writeString(AppState.CURRENT_PHASE, phase.toStateValue());
        }

        List<SessionSlot> slots;
        String dayType;
        // Caixa "novos" e caixa "revisão" são separadas: enquanto o ciclo
        // básico (sequência fixa de plan_items) não terminar, os dois slots
        // do dia vêm só de problemas nunca vistos, na ordem curada (que já
        // varia padrão e dificuldade -- ver seed_problems.json). Revisão
        // espaçada só entra depois que o ciclo básico acaba (fase
        // consolidação/livre); antes disso, um problema resolvido ontem não
        // pode reaparecer hoje só porque "esqueceu 63% segundo a curva" --
        // isso é o bug reportado pelo usuário (Two Sum e Group Anagrams
        // voltando no dia seguinte, no dia 2 de uso, antes de sequer
        // terminar de conhecer os 14 padrões). Uma barra de revisão
        // *opcional* continua disponível em qualquer fase -- ver reviewBar().
        if (phase == Phase.INTRODUCAO) {
            dayType = "aprendizado";
            slots = buildLearningDaySlots(notIntroduced);
        } else {
            dayType = "repeticao_pura";
            List<UrgentReviewItem> queue = reviewQueueService.currentQueue();
            slots = buildPureRepetitionDaySlots(queue, phase);
            writeString(AppState.LAST_PURE_REPETITION_DAY, String.valueOf(currentDay));
        }

        persistDailySession(currentDay, slots);
        return new TodaySession(currentDay, phase, dayType, slots);
    }

    private void persistDailySession(int currentDay, List<SessionSlot> slots) {
        for (int i = 0; i < slots.size(); i++) {
            SessionSlot slot = slots.get(i);
            dailySessionItemRepository.save(
                    new DailySessionItem(currentDay, i, slot.problem(), slot.origin(), slot.urgency())
            );
        }
    }

    /**
     * Sugestões extras de revisão pra mostrar numa barra abaixo da sessão
     * principal, "caso hoje você queira revisar mais alguma coisa" -- em
     * qualquer fase, inclusive durante o ciclo básico, já que aqui é
     * opcional (não substitui nenhum dos 2 slots principais). Exclui
     * problemas que já estão nos slots principais ou que já foram
     * resolvidos hoje (por essa barra, pela sessão principal, ou pelo modo
     * exploração), pra não sugerir de novo algo feito há poucos minutos.
     */
    @Transactional(readOnly = true)
    public List<SessionSlot> reviewBar(TodaySession session, Set<Long> alsoExclude) {
        Set<Long> exclude = new HashSet<>(alsoExclude);
        session.slots().forEach(s -> exclude.add(s.problem().getId()));
        return reviewQueueService.currentQueue().stream()
                .filter(item -> !exclude.contains(item.problem().getId()))
                .limit(REVIEW_BAR_SIZE)
                .map(item -> new SessionSlot(item.problem(), "revisao", item.urgency()))
                .toList();
    }

    private List<SessionSlot> buildLearningDaySlots(List<PlanItem> notIntroduced) {
        List<SessionSlot> slots = new ArrayList<>();
        for (int i = 0; i < Math.min(2, notIntroduced.size()); i++) {
            slots.add(new SessionSlot(notIntroduced.get(i).getProblem(), "novo", null));
        }
        return slots;
    }

    private List<SessionSlot> buildPureRepetitionDaySlots(List<UrgentReviewItem> queue, Phase phase) {
        List<SessionSlot> slots = new ArrayList<>();
        for (int i = 0; i < Math.min(2, queue.size()); i++) {
            UrgentReviewItem item = queue.get(i);
            slots.add(new SessionSlot(item.problem(), "revisao", item.urgency()));
        }

        if (phase == Phase.CONSOLIDACAO) {
            long relevantCount = queue.stream().filter(i -> i.urgency() > CONSOLIDATION_URGENCY_FLOOR).count();
            while (slots.size() < 2 && relevantCount < 2) {
                Optional<Problem> weakSpot = pickWeakestPatternPoolProblem();
                if (weakSpot.isEmpty()) {
                    break;
                }
                slots.add(new SessionSlot(weakSpot.get(), "pool", null));
                relevantCount++; // evita loop infinito se pickWeakestPatternPoolProblem sempre achar algo
            }
        }
        return slots;
    }

    private Optional<Problem> pickWeakestPatternPoolProblem() {
        List<ProblemPatternRepository.PatternEmaProjection> weakestPatterns =
                problemPatternRepository.findPatternsOrderedByAvgEmaQualityAsc();
        for (ProblemPatternRepository.PatternEmaProjection projection : weakestPatterns) {
            Optional<Pattern> pattern = patternRepository.findById(projection.getPatternId());
            if (pattern.isEmpty()) {
                continue;
            }
            List<Problem> unattempted = problemRepository.findUnattemptedPoolByPattern(pattern.get().getSlug());
            if (!unattempted.isEmpty()) {
                return Optional.of(unattempted.get(random.nextInt(unattempted.size())));
            }
            List<Problem> anyPool = problemRepository.findPoolByPattern(pattern.get().getSlug());
            if (!anyPool.isEmpty()) {
                return Optional.of(anyPool.get(random.nextInt(anyPool.size())));
            }
        }
        return Optional.empty();
    }

    /**
     * current_day avança uma vez por dia de calendário (comparado contra
     * last_session_date, guardado em app_state) -- não a cada chamada de
     * /today, pra manter a rota idempotente dentro do mesmo dia. Decisão de
     * design: last_session_date não está listado explicitamente na seção 3
     * do docs/spec/original-spec.md, mas app_state é uma tabela chave-valor genérica, então
     * adicionar essa chave não quebra o schema.
     */
    private void advanceCurrentDayIfNewCalendarDay() {
        LocalDate today = appClock.today();
        Optional<AppState> lastSessionDate = appStateRepository.findById(LAST_SESSION_DATE_KEY);
        if (lastSessionDate.isEmpty()) {
            appStateRepository.save(new AppState(LAST_SESSION_DATE_KEY, today.toString()));
            return;
        }
        LocalDate storedDate = LocalDate.parse(lastSessionDate.get().getValue());
        if (today.isAfter(storedDate)) {
            int currentDay = readInt(AppState.CURRENT_DAY);
            writeString(AppState.CURRENT_DAY, String.valueOf(currentDay + 1));
            lastSessionDate.get().setValue(today.toString());
            appStateRepository.save(lastSessionDate.get());
        }
    }

    private int readInt(String key) {
        return Integer.parseInt(readString(key));
    }

    private String readString(String key) {
        return appStateRepository.findById(key)
                .map(AppState::getValue)
                .orElseThrow(() -> new IllegalStateException("app_state ausente: " + key));
    }

    private void writeString(String key, String value) {
        AppState state = appStateRepository.findById(key).orElse(new AppState(key, value));
        state.setValue(value);
        appStateRepository.save(state);
    }
}
