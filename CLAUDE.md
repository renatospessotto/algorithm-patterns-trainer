# CLAUDE.md: guide for AI coding agents

Spaced-repetition trainer for algorithm patterns. It is a single-user, local app: **Spring Boot 3 (Java 21) + Angular 18 + PostgreSQL 16**, shipped as one Docker image. Read this file first, then the feature doc for whatever you are touching.

## Where to look

| You are working on… | Read | Main code |
|---|---|---|
| What appears on the home screen | [docs/features/daily-session.md](docs/features/daily-session.md) | `SessionPlannerService`, `features/hoje/` |
| Timer, result form, recording | [docs/features/attempt-recording.md](docs/features/attempt-recording.md) | `AttemptsService`, `shared/attempt-card/` |
| Urgency, stability, EMA math | [docs/algorithm.md](docs/algorithm.md), [docs/features/spaced-repetition.md](docs/features/spaced-repetition.md) | `ReviewSchedulerService`, `ReviewQueueService` |
| Random problem / Explore | [docs/features/explore.md](docs/features/explore.md) | `ProblemsService`, `features/explorar/` |
| Pattern progress | [docs/features/patterns-progress.md](docs/features/patterns-progress.md) | `PatternsService`, `ProblemPatternRepository` |
| Stats / heatmap | [docs/features/stats.md](docs/features/stats.md) | `StatsService`, `features/stats/` |
| Attempt history | [docs/features/history.md](docs/features/history.md) | `AttemptsService.history`, `features/historico/` |
| Papers reading feed | [docs/features/reading-feed.md](docs/features/reading-feed.md) | `PapersService`, `features/leitura/` |
| Catalog import / first boot | [docs/features/seed-bootstrap.md](docs/features/seed-bootstrap.md) | `db/migration/V2__Seed_data.java` |
| Problem data, pattern mapping | [docs/etl.md](docs/etl.md) | `tools/etl/generate_seed.py` |
| Endpoints / DTOs | [docs/api.md](docs/api.md) | `src/controller/`, `frontend/src/app/core/models.ts` |
| Tables | [docs/data-model.md](docs/data-model.md) | `db/migration/*.sql`, `src/entity/` |
| Why something is the way it is | [docs/decisions/](docs/decisions/) | — |

## Repo map

- `backend/src/main/java/com/appleetcode/backend/`
  - `config/`: `AppClock`, `SessionProperties`, `WebMvcConfig`.
  - `src/{controller,service,repository,entity}/`: the literal `src` segment in the package name is intentional legacy; follow it.
  - `src/controller/dto/`.
- `backend/src/main/java/db/migration/`: Java Flyway migrations.
- `backend/src/main/resources/`: `application.yml` and `db/migration/*.sql`; `seed/` is generated and git-ignored.
- `frontend/src/app/`: `core/{models.ts,services/}`, `features/<route>/`, `shared/`.
- `tools/etl/`: the seed ETL, its tests and synthetic fixtures.
- `scripts/`, `docs/`.

## Commands

```bash
cd backend && mvn test                         # JUnit
cd frontend && npx ng build                    # type-check + build
python3 -m unittest discover -s tools/etl      # ETL tests (no network)
scripts/seed.sh                                # generate the seed (network on first run)
docker compose up --build -d                   # full app at :8080
```

## Conventions

- **Controllers are thin.** Logic goes in a service, one per feature. Pure algorithm code stays repository-free (`ReviewSchedulerService`) and is covered by plain JUnit.
- **REST paths are declared without `/api`**; `WebMvcConfig` adds the prefix. Every new **Angular route** must also be added to `SpaController`, or refreshes return 404.
- **Keep the frontend models in sync.** When a DTO changes, update `frontend/src/app/core/models.ts` and `docs/api.md`.
- **Dates.** For "today" or any per-day grouping, use `AppClock`, never `LocalDate.now()` or `ZoneId.systemDefault()`.
- **Tunables** go in `application.yml` under `app.*`, bound through `SessionProperties` or `@Value`, not as hard-coded constants.
- **Language.** The domain vocabulary is Portuguese and appears in the API (`hoje`, `novo`, `revisao`, `pool`, `aprendizado`, `repeticao_pura`, `introducao`/`consolidacao`/`livre`). Keep those values. Code comments are Portuguese; docs are English.
- **Frontend.** Standalone components and lazy routes in `app.routes.ts`; HTTP calls only through `core/services/*`.

## Invariants: do not break

1. **Migrations are append-only.** Never edit `V1__schema.sql` or `V3__*.sql` (checksums). Add `V4__...`.
2. **No data in git.** Never commit `backend/src/main/resources/seed/*.json`, ETL caches, `.env` or database dumps. Test fixtures must be synthetic.
3. **The daily session is pinned.** `daily_session_items` makes `GET /api/today` idempotent within a day.
4. **"Introduced" means "has a `review_state` row".** There is no separate counter.
5. **The urgency formula, the EMA weights (0.7/0.3), the growth/time bands and the caps** are specified in `docs/algorithm.md`. Change them only together with the tests and that doc.
6. **The attempt card keeps its `localStorage` pending state** until a successful POST, and surfaces POST errors.
7. **Reading-topic names** in `TOPICS` (ETL) and in `TOPIC_OPTIONS` (`features/leitura`) must match.

## When you change behavior

Update the matching `docs/features/*.md`, keeping the template sections from [`docs/features/_template.md`](docs/features/_template.md). Add an ADR in `docs/decisions/` for decisions that future readers would otherwise reverse. The `/document-feature` command automates the doc update.

## Safety for local work

The owner's real progress lives in the Docker volume `app_leetcode_pgdata`. Never run `docker compose down -v` or `docker volume rm` on the default project. Use `docker compose -p scratch ...` with other ports for experiments (see `docs/operations.md`).
