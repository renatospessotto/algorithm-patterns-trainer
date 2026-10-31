# Feature: Stats (`/stats`)

## Summary
Overall progress: total attempts, average time per difficulty, a GitHub-style consistency heatmap with current and best streaks, and patterns ranked from weakest to strongest.

## User flow
1. Open `/stats`. It loads `GET /api/stats` and `GET /api/patterns`.
2. Click a heatmap day to jump to `/historico?date=YYYY-MM-DD`, which shows only that day's attempts.

## API endpoints
| Method | Path | Purpose |
|---|---|---|
| GET | `/api/stats` | Totals, averages, heatmap |
| GET | `/api/patterns` | Strength ranking |

## Backend
| Class | Path | Role |
|---|---|---|
| `StatsController` | `backend/.../src/controller/StatsController.java` | Endpoint |
| `StatsService.stats` | `backend/.../src/service/StatsService.java` | Aggregates all attempts in memory; groups days by `AppClock.zone()` |
| `StatsResponse` | `backend/.../src/controller/dto/StatsResponse.java` | `{ totalAttempts, avgDurationSecondsByDifficulty, heatmap[] }` |

## Frontend
| Component / service | Path | Role |
|---|---|---|
| `StatsComponent` | `frontend/src/app/features/stats/` | Builds the heatmap weeks, computes streaks client-side, links days to history |
| `StatsService` | `frontend/src/app/core/services/stats.service.ts` | `GET /api/stats` |

## Data
- **R:** `attempts` with `problems.difficulty_label`.

## Invariants & rules
- Heatmap dates are local dates in `app.timezone`, never UTC dates.
- Only days with at least one attempt are returned; the client fills the gaps with empty cells.

## Known pitfalls / past bugs
- **The heatmap looked stale at night.** Days were grouped in the container's UTC. Fixed with `AppClock` ([ADR 0005](../decisions/0005-fixed-app-timezone.md)).
- **Route collision.** The `/stats` API endpoint and the `/stats` SPA route used to share a path. Fixed with the `/api` prefix ([ADR 0004](../decisions/0004-api-path-prefix.md)).

## Tests
—

## Related
[history](history.md) · [patterns-progress](patterns-progress.md)
