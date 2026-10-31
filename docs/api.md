# REST API

All endpoints live under **`/api`**, a prefix added by `WebMvcConfig` to every `@RestController`. JSON in and out, with no authentication. Field names are camelCase. Instants are ISO-8601 UTC strings.

| Method | Path | Controller → Service | Feature doc |
|---|---|---|---|
| GET | `/api/today` | `TodayController` → `SessionPlannerService.today()` | [daily-session](features/daily-session.md) |
| POST | `/api/attempts` | `AttemptsController` → `AttemptsService.record()` | [attempt-recording](features/attempt-recording.md) |
| GET | `/api/attempts` | `AttemptsController` → `AttemptsService.history()` | [history](features/history.md) |
| GET | `/api/queue/review` | `QueueController` → `ReviewQueueService.currentQueueDto()` | [spaced-repetition](features/spaced-repetition.md) |
| GET | `/api/patterns` | `PatternsController` → `PatternsService.progress()` | [patterns-progress](features/patterns-progress.md) |
| GET | `/api/problems/random` | `ProblemsController` → `ProblemsService.random()` | [explore](features/explore.md) |
| GET | `/api/stats` | `StatsController` → `StatsService.stats()` | [stats](features/stats.md) |
| GET | `/api/papers/feed` | `PapersController` → `PapersService.feed()` | [reading-feed](features/reading-feed.md) |

Non-API GETs on `/hoje`, `/explorar`, `/stats`, `/leitura` and `/historico` are forwarded to `index.html` by `SpaController`.

TypeScript mirrors of every shape below live in [`frontend/src/app/core/models.ts`](../frontend/src/app/core/models.ts). Keep the two in sync.

---

### `GET /api/today`
Returns today's session: the 2 pinned slots, the optional review bar, and everything solved today. The **first call of a new calendar day** advances `current_day` and pins the slots, so the call is idempotent within a day.

```jsonc
{
  "dayNumber": 17,
  "phase": "introducao",            // introducao | consolidacao | livre
  "dayType": "aprendizado",         // aprendizado | repeticao_pura
  "slots":     [ /* SessionSlot × ≤2 */ ],
  "reviewBar": [ /* SessionSlot × ≤3, optional extra reviews */ ],
  "solvedToday": [ /* AttemptHistoryItem */ ]
}
```
`SessionSlot` = `{ "problem": Problem, "origin": "novo" | "revisao" | "pool", "urgency": number | null }`.

`Problem` = `{ id, externalSlug, title, leetcodeUrl, difficultyLabel, difficultyRating, isCore }`.

### `POST /api/attempts`
Records an attempt and recomputes the problem's `review_state`.

| Field | Type | Rules |
|---|---|---|
| `problemId` | long | required |
| `durationSeconds` | int | required, > 0 |
| `result` | `AttemptResult` | required |
| `confidence` | int | optional, 1–5 |
| `note` | string | optional |

**201** returns `{ attemptId, problemId, stabilityDays, emaQuality, lastSeenAt, attemptsCount, successesCount }`.

| Status | When | Body |
|---|---|---|
| 400 | Validation fails | Spring default body |
| 404 | Unknown `problemId` | `{ "error": "..." }` |

### `GET /api/attempts`
Full attempt history, most recent first. Returns a list of `AttemptHistoryItem`:
`{ attemptId, problemId, problemTitle, problemSlug, leetcodeUrl, difficultyLabel, attemptedAt, attemptedAtLocalDate, durationSeconds, result, confidence, note }`.

`attemptedAtLocalDate` is the `YYYY-MM-DD` date in `app.timezone`.

### `GET /api/queue/review`
Every problem with a `review_state`, ordered by urgency computed now (descending); ties go to the smaller `stabilityDays`.

Each item is `{ problem: Problem, urgency, stabilityDays, emaQuality, lastSeenAt, attemptsCount, successesCount }`.

### `GET /api/patterns`
Patterns ordered by `orderInPlan` (nulls last). Each item is `{ id, slug, name, orderInPlan, coreTotal, coreSolved, avgEmaQuality | null }`.

### `GET /api/problems/random`
| Query | Default | Meaning |
|---|---|---|
| `pattern` | (none) | Pattern slug. Omit it to draw from the whole pool |
| `exclude_attempted` | `true` | Prefer never-attempted problems. Falls back to the whole pattern pool when every problem has been attempted |

Draws only from the non-core pool (`is_core = false`).

| Status | Body |
|---|---|
| 200 | `Problem` |
| 204 | Nothing found |

### `GET /api/stats`
Returns `{ totalAttempts, avgDurationSecondsByDifficulty: { "Easy": 600.0, ... }, heatmap: [{ date: "YYYY-MM-DD", count }] }`.

Heatmap days are grouped in `app.timezone`.

### `GET /api/papers/feed`
| Query | Meaning |
|---|---|
| `topic` | optional, topic name, case-insensitive |

- **Without `topic`:** refreshes every stale topic and returns all papers.
- **With `topic`:** refreshes and returns only that topic's papers.

Each item is `{ id, title, url, source, venue, publicationYear, citationCount, authors, cachedAt }`, sorted by citations, then by recency of `cachedAt`. An unknown topic returns **404**.
