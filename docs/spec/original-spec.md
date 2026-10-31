# Original specification

> **Status: historical.** This is the planning document the project was built from, translated to English. Section numbers are kept because code comments reference them (`docs/spec/original-spec.md seção N`).
>
> The implementation has since diverged in a few places. **The code and the docs in [`docs/features/`](../features/) are the source of truth.** Known divergences:
>
> | Section | Spec says | Implementation |
> |---|---|---|
> | §2, §10 | The generated `seed_problems.json` is committed to the repo | No generated data is versioned. The ETL runs at Docker build time ([ADR 0007](../decisions/0007-etl-at-build-time-no-data-in-repo.md)) |
> | §3, §6, §8 | Reading feed of dev.to articles (`articles` table, `GET /articles/feed`) | Feed of academic papers from Semantic Scholar (`papers` table, `GET /papers/feed`). See [reading-feed](../features/reading-feed.md) |
> | §3 | — | Extra table `daily_session_items` pins each day's slots ([ADR 0003](../decisions/0003-pin-daily-session.md)). Extra `app_state` key `last_session_date` |
> | §5 | Learning days alternate with pure-repetition days, using a per-week gap | During the introduction phase every day is a learning day with 2 new problems. Spaced review is offered as an optional "review bar". `pure-repetition-gap-by-week` is still configurable but currently unused by the planner. See [daily-session](../features/daily-session.md) |
> | §6 | Endpoints at `/today`, `/attempts`, … | Every REST endpoint is prefixed with `/api` ([ADR 0004](../decisions/0004-api-path-prefix.md)). Extra endpoint `GET /api/attempts` (history) |
> | §7 | 4 routes | 5 routes: `/historico` was added |
> | §9 | Seed loaded by a `CommandLineRunner` or a Flyway data migration | Flyway Java migration `V2__Seed_data` ([ADR 0002](../decisions/0002-seed-as-flyway-java-migration.md)) |

---

## 1. Stack

The backend is Spring Boot with Spring Data JPA and the `org.postgresql:postgresql` driver. Hibernate uses its native `PostgreSQLDialect`. Migrations use Flyway, and endpoints are plain `@RestController`s.

There is no Spring Security: the app is meant for single-user, local use.

The frontend is Angular with standalone components. It uses `HttpClient` to talk to the API, and Angular Material for forms, the timer and the dashboard.

- **During development**, `ng serve` runs with a proxy to `localhost:8080`, which gives hot reload.
- **For daily use**, the Angular production build is packaged inside the Spring Boot artifact, so a single app container is deployed.

## 2. Problem data sources

Instead of hand-curating a list, the app merges public datasets:

1. **`neenza/leetcode-problems`:** about 2,900 problems in JSON, with the official LeetCode `topics` tags (Array, Two Pointers, Dynamic Programming, …). It is the main source of title, slug and difficulty.
2. **`zerotrac/leetcode_problem_rating`:** `ratings.txt`, an Elo-like numeric rating for about 2,300 problems. It gives a continuous difficulty instead of only Easy/Medium/Hard.
3. **Sean Prashad's "LeetCode Patterns" list:** used as a reference to validate how the granular official tags are grouped into macro-patterns. Tags like "Array" appear on almost everything.

Seed generation process, run once, outside the container:

1. Download both datasets.
2. Merge them by slug and keep `title, slug, difficulty_label, difficulty_rating, topics_raw`.
3. Apply a hand-curated mapping from official tags to one of **14 macro-patterns**. The mapping is small: about 40–50 tag combinations cover most problems.
4. Mark the problems of the fixed phase-1 sequence with `is_core = true` (Two Sum, Number of Islands, …). The rest of the dataset is the pool for random exploration.
5. Write `seed_problems.json`.

On first boot, with an empty database, the backend reads this local JSON and populates Postgres. Nothing is downloaded at runtime.

## 3. Database schema

- `patterns`: `id, name, slug, description, order_in_plan`. `order_in_plan` is null when the pattern exists only in the exploration pool.
- `problems`: `id, external_slug, title, leetcode_url, difficulty_label, difficulty_rating, is_core`.
- `problem_patterns`: many-to-many, `problem_id, pattern_id, is_primary`. A problem can cover more than one pattern; Course Schedule II is both graph and topological sort.
- `plan_items`: the fixed phase-1 sequence, `day_number, pattern_id, problem_id, slot ("novo")`.
- `attempts`: a log of every attempt, `id, problem_id, attempted_at, duration_seconds, result, confidence (1–5, optional), note`.
- `review_state`: current state per problem, with no fixed next-review date. Columns: `problem_id (PK), stability_days, ema_quality, last_seen_at, attempts_count, successes_count`.
- `app_state`: key-value pairs, `current_phase (introducao / consolidacao / livre), current_day, last_pure_repetition_day`.
- `topics`: general reading topics, unrelated to algorithm patterns.
- `articles`: cache of recommended reading per topic.

## 4. Algorithm: a continuous priority queue instead of fixed dates

Fixed review boxes (review in 1, 3, 7, 14 days) do not work with a capacity of 2 exercises per day. Dates eventually collide, and the system cannot prioritize, or items wait weeks because the fixed queue ignores real capacity.

So no problem has a "next review date". Each problem has a `stability_days`, the current estimate of how many days it takes to really be forgotten, and a `last_seen_at`. Urgency is computed on demand with the classic forgetting curve:

```
urgency = 1 - exp(-days_since_last_seen / stability_days)
```

Urgency grows continuously and never exceeds 1, so there is no backlog explosion. After a week away, the oldest items sit near the ceiling, and the queue serves the 2 most urgent ones.

Every attempt feeds an exponential moving average of quality, not just the last result:

```
ema_quality_new = 0.7 × ema_quality_old + 0.3 × q
```

`q` is a 0–5 scale: 0 means gave up, 5 means solved comfortably and quickly while naming the pattern.

**Updating `stability_days` after each attempt:**

- **If `q < 3`** (failed, or needed a heavy hint): `stability_days` is halved, with a minimum of 1 day.
- **If `q ≥ 3`**: `stability_days` is multiplied by a growth factor based on the *updated* `ema_quality`:

  | `ema_quality` | Growth factor |
  |---|---|
  | [3, 4) | × 1.3 |
  | [4, 5) | × 1.8 |
  | 5 | × 2.3 |

  It is also multiplied by a time factor based on `time_ratio`, the time spent divided by the target time for the difficulty (12 / 22 / 38 min):

  | `time_ratio` | Time factor |
  |---|---|
  | ≤ 1 | × 1.15 |
  | > 1.5 | × 0.75 |
  | otherwise | × 1.0 |

`stability_days` is capped by difficulty: easy 90 days, medium 45, hard 20. The first exposure always starts at `stability_days = 1`, which forces a quick second pass.

**Daily queue:** compute the urgency of every problem with a `review_state`, sort descending, and break ties by smaller `stability_days` first, so the most fragile item wins. There is no "overdue" concept, only priority order.

## 5. Phases and daily flow

Each day is either a learning day or a pure-repetition day. The maximum number of consecutive learning days grows over time:

| Weeks | Max consecutive learning days |
|---|---|
| 1–2 | 1 |
| 3–4 | 2 |
| 5–6 | 3 |
| 7+ | 4 |

This is a configuration value.

- **Learning day:** slot 1 gets the next new problem of the fixed sequence. Slot 2 gets the most urgent review item, or another new problem if the review queue is still empty.
- **Pure-repetition day:** both slots come from the top of the urgency queue, and `last_pure_repetition_day` is updated.
- **Consolidation phase:** starts automatically when the fixed sequence ends. If fewer than 2 queue items have urgency above 0.15, the remaining slot is filled from the exploration pool of the weakest pattern, meaning the pattern with the lowest average `ema_quality`.
- **Free phase:** chosen manually. The "Explore" screen offers a pattern picker and a "random exercise" button over the non-core pool, preferring problems not yet attempted.

## 6. API endpoints

- `GET /today`: today's session, already resolved by the phase logic.
- `POST /attempts`: `problem_id, duration_seconds, result, confidence, note`. Stores the attempt and recomputes `review_state`.
- `GET /patterns`: patterns with progress.
- `GET /queue/review`: problems ordered by urgency computed now.
- `GET /problems/random?pattern=&exclude_attempted=true`: exploration mode.
- `GET /stats`: history, average time per difficulty, and a GitHub-style heatmap.
- `GET /articles/feed?topic=`: cached reading feed, refreshed from the external API when stale.

## 7. Frontend

Routes:

- **`/hoje`:** today's session, with a timer component and a reactive result form.
- **`/explorar`:** pattern picker plus a random button.
- **`/stats`:** progress, weak patterns, consistency heatmap.
- **`/leitura`:** a general reading feed.

Services group the HTTP calls per endpoint group.

## 8. Reading recommendations

The reading feed is separate from pattern study. Topics are stored in `topics`. The feed endpoint behaves as follows:

- **Fresh cache:** if cached entries younger than **14 days** exist for the topic, return them.
- **Stale or empty cache:** query the external API, dedupe by URL, store with `cached_at = now` and return.
- **External API fails:** return the stale cache. The result is empty only if nothing was ever fetched for that topic.

## 9. Containerization

`docker-compose.yml` defines two services, `app` and `db`:

- **`db`:** `postgres:16-alpine`, with a named volume so data survives `docker compose down` / `up`. It is only exposed to the host for debugging.
- **`app`:** a multi-stage Dockerfile.
  1. Node builds the Angular app.
  2. Maven builds Spring Boot, with the Angular output copied into `src/main/resources/static`.
  3. A JRE-only runtime runs the jar.

  `app` waits for `db` to be healthy (`pg_isready`) and receives the JDBC URL via environment.

Flyway runs at startup. With an empty database, the seed is imported before the app accepts requests. For frontend development, `proxy.conf.json` forwards `/api` to the backend.

## 10. Suggested build order

1. Generate the seed and the reading topics.
2. Create the Spring Boot project and the schema migrations.
3. Start the `db` service and validate the connection.
4. Implement the seed bootstrap.
5. Implement the priority-queue algorithm as an isolated, unit-tested `ReviewSchedulerService`.
6. Implement the REST endpoints, including the cached reading feed.
7. Create the Angular project and its routes.
8. Write the multi-stage Dockerfile and complete `docker-compose.yml`.
9. Test a full day end to end: open the session, solve, record, check that the next day's queue changes, and check that the reading feed uses the cache on the second visit.
