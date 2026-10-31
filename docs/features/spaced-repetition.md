# Feature: Spaced repetition (review queue)

## Summary
Problems you have attempted are re-surfaced by **urgency**, an estimate of how much you have forgotten them. There are no fixed due dates, so the queue always serves the most urgent items within the 2-per-day capacity.

## User flow
1. Every recorded attempt updates the problem's `stability_days` and `ema_quality`.
2. Urgency is recomputed on every read, so it grows continuously with time.
3. Reviews show up in the main slots (consolidation and free phases) and in the optional review bar on `/hoje` (all phases).

## API endpoints
| Method | Path | Purpose |
|---|---|---|
| GET | `/api/queue/review` | The full queue ordered by urgency ([api.md](../api.md#get-apiqueuereview)) |

## Backend
| Class | Path | Role |
|---|---|---|
| `ReviewSchedulerService` | `backend/.../src/service/ReviewSchedulerService.java` | `calculateUrgency`, `recordAttempt`, growth and time factors. Repository-free |
| `ReviewQueueService` | `backend/.../src/service/ReviewQueueService.java` | Loads all `review_state` rows, computes urgency now, sorts |
| `QueueController` | `backend/.../src/controller/QueueController.java` | Endpoint |
| `SessionProperties` | `backend/.../config/SessionProperties.java` | Time targets and caps from `application.yml` |
| `UrgentReviewItem`, `ReviewQueueItemDto` | `src/service`, `src/controller/dto` | Shapes |

## Frontend
| Component / service | Path | Role |
|---|---|---|
| `PatternsService.getReviewQueue` | `frontend/src/app/core/services/patterns.service.ts` | `GET /api/queue/review` |
| `HojeComponent` review bar | `frontend/src/app/features/hoje/` | Shows up to 3 extra reviews |

## Data
- **R:** `review_state` joined with `problems`.
- **W:** `review_state`, through attempt recording.

## Invariants & rules
- Urgency stays in `[0, 1)`; stability is ≥ 1 day and ≤ the difficulty cap (90 / 45 / 20).
- The first exposure always sets stability to 1, whatever the result.
- Growth uses the **updated** EMA, not the single result.
- Ordering is urgency descending, then stability ascending.
- Full formulas: [algorithm.md](../algorithm.md).

## Known pitfalls / past bugs
- The queue is computed in memory over all `review_state` rows. That is fine for one user (at most a few thousand rows); revisit before multi-user use.

## Tests
`ReviewSchedulerServiceTest`: 20+ cases covering the curve, every factor band, the caps, the EMA and the counters. Run with `cd backend && mvn test`.

## Related
[ADR 0001](../decisions/0001-continuous-priority-queue.md) · [daily-session](daily-session.md) · [attempt-recording](attempt-recording.md)
