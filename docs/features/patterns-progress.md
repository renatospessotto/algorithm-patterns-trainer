# Feature: Pattern progress

## Summary
For each of the 14 macro-patterns: how many core problems it has, how many you have solved, and your average quality (EMA) on it. This feeds the pattern picker in Explore, the weak/strong ranking in Stats, and the "weakest pattern" choice in consolidation.

## User flow
1. Shown as the pattern list in `/explorar` and as the strength ranking on `/stats` (weakest first).
2. No direct user action; it updates as attempts are recorded.

## API endpoints
| Method | Path | Purpose |
|---|---|---|
| GET | `/api/patterns` | `PatternProgressDto[]` ordered by `orderInPlan` |

## Backend
| Class | Path | Role |
|---|---|---|
| `PatternsController` | `backend/.../src/controller/PatternsController.java` | Endpoint |
| `PatternsService.progress` | `backend/.../src/service/PatternsService.java` | Assembles the DTOs |
| `ProblemPatternRepository` | `backend/.../src/repository/ProblemPatternRepository.java` | `countCoreByPattern`, `countCoreSolvedByPattern`, `avgEmaQualityByPattern`, `findPatternsOrderedByAvgEmaQualityAsc` (used by consolidation) |

## Frontend
| Component / service | Path | Role |
|---|---|---|
| `PatternsService.getPatterns` | `frontend/src/app/core/services/patterns.service.ts` | `GET /api/patterns` |
| `StatsComponent` | `frontend/src/app/features/stats/` | Sorts by `avgEmaQuality` ascending and maps it to a strength class |
| `ExplorarComponent` | `frontend/src/app/features/explorar/` | Picker |

## Data
- **R:** `patterns`, `problem_patterns`, `problems.is_core`, `review_state.ema_quality` and `successes_count`.

## Invariants & rules
- `coreSolved` counts core problems of the pattern with at least one success (`successes_count > 0`), not merely attempted ones.
- `avgEmaQuality` averages over **every attempted** problem that covers the pattern (core and pool, primary or secondary). It is `null` when none has been attempted.

## Known pitfalls / past bugs
- The service issues 3 queries per pattern (42 in total). That is acceptable for 14 patterns; batch them if the number of patterns grows.

## Tests
—

## Related
[stats](stats.md) · [explore](explore.md) · [daily-session](daily-session.md) (consolidation)
