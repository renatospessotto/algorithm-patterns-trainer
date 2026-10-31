# 0001: Continuous urgency queue instead of fixed review dates

**Status:** Accepted

## Context
Classic spaced-repetition boxes (review in 1, 3, 7, 14 days) assign due dates. With a fixed capacity of 2 exercises per day, due dates collide: some days have 10 items due and the system cannot say which matter most. After a break, the backlog explodes.

## Decision
Store no due date. Each problem keeps `stability_days` and `last_seen_at`, and its urgency `1 − exp(−Δdays / stability)` is computed on read. Each day takes the top of the queue (ties go to the lower stability). Stability shrinks on failure and grows on success, driven by an EMA of quality and by speed relative to a per-difficulty target, and is capped per difficulty. See [algorithm.md](../algorithm.md).

## Consequences
- Capacity-aware by construction; there is no "overdue" state and no backlog explosion.
- Urgency must be recomputed for every reviewed problem on each read. That is cheap for one user.
- The math lives in a repository-free service, so it is fully unit-tested.
