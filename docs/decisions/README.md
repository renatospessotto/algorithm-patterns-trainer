# Architecture Decision Records

Short records of the decisions that shape the code. Format: **Context → Decision → Consequences**. Add a new numbered file instead of rewriting an accepted one; mark superseded records as such.

| # | Decision | Status |
|---|---|---|
| [0001](0001-continuous-priority-queue.md) | Continuous urgency queue instead of fixed review dates | Accepted |
| [0002](0002-seed-as-flyway-java-migration.md) | Seed import as a Flyway Java migration | Accepted |
| [0003](0003-pin-daily-session.md) | Pin each day's slots in `daily_session_items` | Accepted |
| [0004](0004-api-path-prefix.md) | Prefix every REST endpoint with `/api` | Accepted |
| [0005](0005-fixed-app-timezone.md) | One configured timezone via `AppClock` | Accepted |
| [0006](0006-persist-pending-attempt-client-side.md) | Persist in-progress attempts in `localStorage` | Accepted |
| [0007](0007-etl-at-build-time-no-data-in-repo.md) | Generate the seed at build time; commit no data | Accepted |
