# Scheduling algorithm

There are two layers:

1. **[`ReviewSchedulerService`](../backend/src/main/java/com/appleetcode/backend/src/service/ReviewSchedulerService.java)**: pure math. It computes urgency and updates `review_state` after an attempt. It never touches repositories, so it is unit-tested with plain JUnit.
2. **[`SessionPlannerService`](../backend/src/main/java/com/appleetcode/backend/src/service/SessionPlannerService.java)**: decides what goes into each day's 2 slots, based on the phase. See [features/daily-session.md](features/daily-session.md).

## 1. Urgency: a continuous priority, no due dates

```
urgency = 1 − exp(−days_since_last_seen / stability_days)
```

- The value is always in `[0, 1)`: 0 right after an attempt, approaching 1 as time passes.
- `stability_days` is floored at 1 to avoid dividing by zero. Negative elapsed time is clamped to 0.
- Elapsed time uses minutes / 1440, so it is fractional.
- **Ordering** ([`ReviewQueueService`](../backend/src/main/java/com/appleetcode/backend/src/service/ReviewQueueService.java)): urgency descending, and on ties smaller `stability_days` first (the most fragile wins).

There is no "overdue" state. After a week away, the most urgent items sit near 1 and are served first, with no backlog explosion.

## 2. Updating state after an attempt

Inputs: `q` from `AttemptResult` (0–5, see [data-model.md](data-model.md#enums)), `durationSeconds`, and the problem's `difficulty_label`.

**EMA of quality:**

```
first exposure:  ema = q
otherwise:       ema = 0.7 × ema_old + 0.3 × q
```

**Stability:**

```
first exposure:  stability = 1                                   (forces a quick second pass)
q < 3:           stability = max(1, stability / 2)
q ≥ 3:           stability = min(cap, stability × growth(ema_new) × timeFactor)
```

The growth factor uses the **new** EMA, not the single `q`:

| `ema_new` | `growth` |
|---|---|
| ≥ 5 | 2.3 |
| [4, 5) | 1.8 |
| [3, 4) | 1.3 |
| < 3 | 1.0 (neutral; the spec does not define this band) |

The time factor uses `time_ratio = durationSeconds / target(difficulty)`:

| `time_ratio` | `timeFactor` |
|---|---|
| ≤ 1.0 | 1.15 |
| (1.0, 1.5] | 1.0 |
| > 1.5 | 0.75 |

**Counters:** `attempts_count += 1` always; `successes_count += 1` when `q ≥ 3`; `last_seen_at = now`.

## 3. Tunables (`application.yml` → `app.session.*`)

| Key | Default | Used by |
|---|---|---|
| `difficulty-time-target-seconds` | easy 720 · medium 1320 · hard 2280 (12 / 22 / 38 min) | `timeFactorFor` |
| `difficulty-stability-cap-days` | easy 90 · medium 45 · hard 20 | `recordAttempt` cap |
| `pure-repetition-gap-by-week` | weeks 1–2: 1 · 3–4: 2 · 5–6: 3 · default: 4 | Loaded by `SessionProperties`; **currently unused by the planner** (see daily-session) |

The keys are bound by [`SessionProperties`](../backend/src/main/java/com/appleetcode/backend/config/SessionProperties.java). Difficulty lookup is lowercase.

## 4. Session-level constants (`SessionPlannerService`)

| Constant | Value | Meaning |
|---|---|---|
| Slots per day | 2 | The main session size |
| `REVIEW_BAR_SIZE` | 3 | Maximum number of optional extra review suggestions |
| `CONSOLIDATION_URGENCY_FLOOR` | 0.15 | In consolidation, queue items at or below this urgency do not count as "relevant", and the slot is filled from the weakest pattern's pool instead |

## Tests

[`ReviewSchedulerServiceTest`](../backend/src/test/java/com/appleetcode/backend/src/service/ReviewSchedulerServiceTest.java) covers:

- the urgency curve: 0 at t=0, never reaching 1, the classic forgetting-curve value, and higher stability giving lower urgency;
- first exposure;
- halving and its floor;
- every growth band and time-factor band;
- the caps per difficulty;
- the EMA weighting;
- the counters.

Run with `cd backend && mvn test`.
