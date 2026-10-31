# Feature: Daily session (`/hoje`)

## Summary
The home screen shows **today's 2 exercises**, chosen by the phase logic and pinned for the whole day. It also shows an optional bar with up to 3 extra reviews, and the list of everything solved today.

## User flow
1. Open `/hoje`. The page calls `GET /api/today`.
2. On the first call of a new calendar day, the backend advances `current_day` and decides that day's slots.
3. Each slot is an [attempt card](attempt-recording.md): open on LeetCode, run the timer, record the result.
4. After recording, the page reloads `/api/today`. The card shows "Já resolvido hoje", the problem moves into "Resolvidos hoje", and the review bar recalculates without it.
5. The pinned slots do not change for the rest of the day, solved or not.

## API endpoints
| Method | Path | Purpose |
|---|---|---|
| GET | `/api/today` | Session + review bar + solved today ([api.md](../api.md#get-apitoday)) |

## Backend
| Class | Path | Role |
|---|---|---|
| `TodayController` | `backend/.../src/controller/TodayController.java` | Thin endpoint |
| `SessionPlannerService` | `backend/.../src/service/SessionPlannerService.java` | Day advance, phase decision, slot building, pinning, review bar |
| `PlanItemRepository.findNextNotIntroduced` | `backend/.../src/repository/PlanItemRepository.java` | The next plan items whose problem has no `review_state` |
| `DailySessionItem` / repository | `backend/.../src/entity/DailySessionItem.java` | Pinned slots per `day_number` |
| `ReviewQueueService` | `backend/.../src/service/ReviewQueueService.java` | Urgency-ordered queue for repetition days and the review bar |
| `AppClock` | `backend/.../config/AppClock.java` | Today's date and the "solved today" window |
| `TodaySession`, `SessionSlot`, `TodayResponse` | `src/service`, `src/controller/dto` | Internal and wire shapes |

## Frontend
| Component / service | Path | Role |
|---|---|---|
| `HojeComponent` | `frontend/src/app/features/hoje/` | Page. Renders slots, the review bar and solved today. `trackByProblem` keeps cards alive across reloads |
| `AttemptCardComponent` | `frontend/src/app/shared/attempt-card/` | One per slot |
| `TodayService` | `frontend/src/app/core/services/today.service.ts` | `GET /api/today` |

## Data
- **R:** `app_state`, `plan_items`, `review_state`, `problems`, `attempts` (today's window), `problem_patterns`.
- **W:** `app_state.current_day`, `last_session_date`, `current_phase`, `last_pure_repetition_day`; `daily_session_items`.

## Decision logic (`buildTodaySession`)
```
advance current_day if today (AppClock) > last_session_date
if daily_session_items exist for current_day → return them (pinned)
notIntroduced = plan items without review_state, in day_number order
if phase == introducao and notIntroduced is empty → phase = consolidacao
if phase == introducao:
    dayType = aprendizado; slots = first 2 of notIntroduced (origin "novo")
else:
    dayType = repeticao_pura; slots = top 2 of the urgency queue (origin "revisao")
    if consolidacao and fewer than 2 relevant items (urgency > 0.15):
        fill from the weakest pattern's pool (origin "pool"), preferring unattempted
    last_pure_repetition_day = current_day
persist slots to daily_session_items
```
**Review bar:** the top 3 of the urgency queue, excluding today's slots and anything already attempted today. It is offered in **every** phase and is always optional.

## Invariants & rules
- `GET /api/today` is idempotent within a calendar day: the same slots, in the same order.
- "Introduced" means "has a `review_state`". There is no separate counter.
- During `introducao`, review never takes a main slot. A problem seen yesterday must not displace new material (it is available in the review bar instead).
- `current_day` counts **days the app was opened**, not calendar days elapsed.

## Known pitfalls / past bugs
- **Slots changing mid-day.** Recomputing slots on every call made a just-solved problem "disappear" and be replaced. Slots are now pinned in `daily_session_items` ([ADR 0003](../decisions/0003-pin-daily-session.md)).
- **Reviews leaking into day 2.** Two Sum and Group Anagrams came back the day after first seeing them, before the basic cycle ended. Fixed by keeping the main slots new-only during `introducao`.
- **The same 2 problems shown for days.** This is expected when they are never recorded: the slots are "the next plan items without `review_state`". The attempt card now survives reloads and surfaces errors ([attempt-recording](attempt-recording.md)).
- **Day rollover in UTC.** Night-time attempts landed on the wrong day before `AppClock` existed ([ADR 0005](../decisions/0005-fixed-app-timezone.md)).
- **Divergence from the spec:** `pure-repetition-gap-by-week` (alternating learning and pure-repetition days) is configured but not used by the current planner.

## Tests
[`SessionPlannerServiceTest`](../../backend/src/test/java/com/appleetcode/backend/src/service/SessionPlannerServiceTest.java) checks that day 1 is a learning day with new problems, and that day 2 stays in learning mode while the cycle is unfinished.

## Related
[spaced-repetition](spaced-repetition.md) · [attempt-recording](attempt-recording.md) · [ADR 0001](../decisions/0001-continuous-priority-queue.md) · [ADR 0003](../decisions/0003-pin-daily-session.md)
