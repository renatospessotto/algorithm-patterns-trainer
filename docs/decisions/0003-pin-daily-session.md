# 0003: Pin each day's slots in `daily_session_items`

**Status:** Accepted

## Context
`GET /today` originally recomputed the slots on every call. Solving a "new" problem made it introduced, so the next call silently swapped in a different problem, and random pool picks changed on every refresh. The exercise you had just done "disappeared" from the home screen.

## Decision
On the first `GET /today` of a `day_number`, persist the chosen slots to `daily_session_items` (`UNIQUE(day_number, slot_index)`). Later calls that day return the pinned slots.

## Consequences
- Idempotent within a day; solved slots stay visible and are marked done.
- Changing the planner logic affects only future days.
- Extra table, added in `V3`.
