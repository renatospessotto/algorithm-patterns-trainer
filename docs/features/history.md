# Feature: History (`/historico`)

## Summary
The full log of attempts, most recent first, filterable by difficulty and by a specific day. The day filter is the target of heatmap clicks on `/stats`.

## User flow
1. Open `/historico` (or come from a heatmap day as `/historico?date=YYYY-MM-DD`).
2. Filter by difficulty chip, or clear the date filter.
3. Each entry shows the problem link, its difficulty, the result label, the duration and a collapsible note.

## API endpoints
| Method | Path | Purpose |
|---|---|---|
| GET | `/api/attempts` | All attempts, newest first ([api.md](../api.md#get-apiattempts)) |

## Backend
| Class | Path | Role |
|---|---|---|
| `AttemptsController.history` | `backend/.../src/controller/AttemptsController.java` | Endpoint |
| `AttemptsService.history` | `backend/.../src/service/AttemptsService.java` | Maps to `AttemptHistoryItemDto` with `attemptedAtLocalDate` |
| `AttemptRepository.findAllWithProblemOrderByAttemptedAtDesc` | `backend/.../src/repository/AttemptRepository.java` | Fetch-joins the problem |
| `SpaController` | `backend/.../src/controller/SpaController.java` | Forwards `/historico` so that refresh and deep links work |

## Frontend
| Component / service | Path | Role |
|---|---|---|
| `HistoricoComponent` | `frontend/src/app/features/historico/` | Filters, reads `?date=` |
| `ClampedNoteComponent` | `frontend/src/app/shared/clamped-note/` | Long notes with expand/collapse |
| `resultLabel` / `resultClass` | `frontend/src/app/shared/attempt-result.ts` | Display helpers |

## Data
- **R:** `attempts` joined with `problems`.

## Invariants & rules
- The date filter compares against `attemptedAtLocalDate`, which the server computes in `app.timezone`, so it matches the heatmap exactly.

## Known pitfalls / past bugs
- `/historico` was missing from `SpaController`, so a refresh on that page returned 404. Fixed; **every new route must be added there**.

## Tests
—

## Related
[stats](stats.md) · [attempt-recording](attempt-recording.md)
