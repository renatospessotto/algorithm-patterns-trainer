# Feature: Attempt recording

## Summary
Every problem card (on `/hoje`, the review bar and `/explorar`) lets you open the problem, time yourself and record the result. Recording updates the problem's spaced-repetition state.

## User flow
1. **Abrir no LeetCode** opens the problem in a new tab and starts the timer. The start time is saved in `localStorage`.
2. **Terminei** stops the timer and opens the form. The minutes field is prefilled from the timer and stays editable.
3. Alternatively, **Já resolvi, registrar sem cronômetro** goes straight to the form, where you type the minutes.
4. Pick the result (6 levels), an optional confidence (1–5) and an optional note, then **Registrar tentativa**.
5. On success the card shows the new stability and EMA. On failure it shows an error and keeps the form for a retry.

## API endpoints
| Method | Path | Purpose |
|---|---|---|
| POST | `/api/attempts` | Record the attempt and update `review_state` ([api.md](../api.md#post-apiattempts)) |

## Backend
| Class | Path | Role |
|---|---|---|
| `AttemptsController` | `backend/.../src/controller/AttemptsController.java` | `@Valid` request |
| `AttemptsService.record` | `backend/.../src/service/AttemptsService.java` | Saves `Attempt`, then calls the scheduler and saves `ReviewState`, in one transaction |
| `ReviewSchedulerService.recordAttempt` | `backend/.../src/service/ReviewSchedulerService.java` | The math ([algorithm.md](../algorithm.md)) |
| `AttemptRequest` / `AttemptResponse` | `backend/.../src/controller/dto/` | Wire shapes, with validation |
| `AttemptResult` | `backend/.../src/entity/AttemptResult.java` | Result → q (0–5) |

## Frontend
| Component / service | Path | Role |
|---|---|---|
| `AttemptCardComponent` | `frontend/src/app/shared/attempt-card/` | Stage machine `idle → timing → form → done`; pending state in `localStorage` (`pending-attempt:<problemId>`); error handling |
| `TimerComponent` | `frontend/src/app/shared/timer/` | Counts from an `@Input() startedAt`, so it resumes after a reload |
| `AttemptsService` | `frontend/src/app/core/services/attempts.service.ts` | `POST /api/attempts` |
| `RESULT_OPTIONS` | `frontend/src/app/shared/attempt-result.ts` | Labels for the 6 results |

## Data
- **W:** `attempts` (insert), `review_state` (insert or update).
- **R:** `problems`.

## Invariants & rules
- The frontend sends `durationSeconds = minutes × 60` (minimum 1). The duration drives `time_ratio`, so a wrong value inflates or deflates stability.
- The pending `localStorage` entry is cleared only after a **successful** POST, or when the problem is already done today.
- An `alreadyDone` input (from `solvedToday`) always wins: the card shows "done".

## Known pitfalls / past bugs
- **Attempts silently lost** (fixed). The timer lived only in component memory: a page reload, switching app tabs, or the browser discarding the tab while you solved on LeetCode reset the card to "idle". `submit()` also had no error handler, so a failed POST looked like nothing happened. Recording one card also re-rendered the page and killed the other card's running timer. The fixes are the `localStorage` pending state, a visible error with retry, `trackBy` in `HojeComponent`, and manual entry ([ADR 0006](../decisions/0006-persist-pending-attempt-client-side.md)).

## Tests
- Backend math: `ReviewSchedulerServiceTest`.
- The card has no unit tests. Verify manually (or headless) by reloading during timing, simulating a POST failure, and doing a manual entry.

## Related
[spaced-repetition](spaced-repetition.md) · [daily-session](daily-session.md) · [ADR 0006](../decisions/0006-persist-pending-attempt-client-side.md)
