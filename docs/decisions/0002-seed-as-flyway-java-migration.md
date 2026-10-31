# 0002: Seed import as a Flyway Java migration

**Status:** Accepted

## Context
The seed was first imported by an `ApplicationRunner`. Spring calls runners only *after* the embedded Tomcat is already accepting connections. A `GET /today` arriving in that window saw an empty `plan_items` table, concluded that the fixed sequence was finished, and permanently switched the phase to `consolidacao`.

## Decision
Import the seed in `V2__Seed_data`, a Flyway `BaseJavaMigration`. Flyway runs while the `DataSource` initializes, before the web server starts.

## Consequences
- There is no window where requests see a half-initialized database.
- The seed runs exactly once per database; changing the seed later needs a new migration or a fresh volume.
- If the seed file is missing, the migration throws. Postgres rolls the migration back, so the next boot retries.
