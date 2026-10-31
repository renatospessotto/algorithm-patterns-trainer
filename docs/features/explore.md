# Feature: Explore (`/explorar`)

## Summary
Draw a random problem from the **non-core pool** (about 2,900 problems), optionally filtered by pattern and preferring problems you have never attempted. The drawn problem can be timed and recorded like any other.

## User flow
1. Open `/explorar`; the pattern list comes from `GET /api/patterns`.
2. Optionally pick a pattern, then click the random button. This calls `GET /api/problems/random`.
3. The result is rendered as an attempt card (badge "Aleatório"). Recording it updates `review_state`, so the problem joins the review queue.
4. Patterns are refreshed after recording, so their progress stays current.

## API endpoints
| Method | Path | Purpose |
|---|---|---|
| GET | `/api/problems/random?pattern=&exclude_attempted=true` | Random pool problem (204 when none) |
| GET | `/api/patterns` | Pattern picker |
| POST | `/api/attempts` | Via the attempt card |

## Backend
| Class | Path | Role |
|---|---|---|
| `ProblemsController` | `backend/.../src/controller/ProblemsController.java` | 200 / 204 |
| `ProblemsService.random` | `backend/.../src/service/ProblemsService.java` | Candidate selection with fallback |
| `ProblemRepository` | `backend/.../src/repository/ProblemRepository.java` | `findPool`, `findUnattemptedPool`, `findPoolByPattern`, `findUnattemptedPoolByPattern` |

## Frontend
| Component / service | Path | Role |
|---|---|---|
| `ExplorarComponent` | `frontend/src/app/features/explorar/` | Picker, draw, card |
| `ProblemsService` | `frontend/src/app/core/services/problems.service.ts` | `random(pattern?, excludeAttempted=true)` |
| `PatternsService` | `frontend/src/app/core/services/patterns.service.ts` | `getPatterns()` |

## Data
- **R:** `problems` (`is_core = false`), `problem_patterns`, `patterns`, `review_state` (to exclude attempted problems).

## Invariants & rules
- Core problems are never drawn here; they belong to the fixed sequence.
- With `exclude_attempted=true`, if every problem of the pattern was attempted, the draw falls back to the whole pattern pool instead of returning nothing.
- The same card instance is reused across draws; `AttemptCardComponent.ngOnChanges` resets its stage when `problem` changes.

## Known pitfalls / past bugs
- Explore originally had no way to record an attempt, so random problems never showed up anywhere. Fixed by reusing the shared attempt card.

## Tests
—

## Related
[attempt-recording](attempt-recording.md) · [patterns-progress](patterns-progress.md)
