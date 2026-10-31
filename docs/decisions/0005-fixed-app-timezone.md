# 0005: One configured timezone via `AppClock`

**Status:** Accepted

## Context
Docker images run in UTC. Day boundaries (advancing `current_day`, "solved today", the heatmap) used `ZoneId.systemDefault()`. Evening attempts in UTC−3 landed on the next UTC day, so "today" looked stale.

## Decision
`AppClock` is the single source of "today", configured by `app.timezone` (env `APP_TIMEZONE`, default `America/Sao_Paulo`). Instants stay UTC in the database; only grouping by day uses the zone.

## Consequences
- Day logic is independent of the host or container TZ.
- New code must use `AppClock`, never `LocalDate.now()` or `ZoneId.systemDefault()`.
