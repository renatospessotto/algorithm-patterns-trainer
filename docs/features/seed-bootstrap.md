# Feature: Seed bootstrap

## Summary
On the first boot against an empty database, the catalog (patterns, problems, pattern links, the fixed plan, and the reading topics) is imported from the ETL output. The import happens inside Flyway, before the web server accepts requests.

## User flow
1. `docker compose up --build`. The Docker build runs the ETL, and the seed JSON is baked into the jar.
2. On boot, Flyway applies `V1` (schema), `V2` (seed) and `V3`.
3. On later boots, Flyway sees `V2` already applied and does nothing.

## API endpoints
—

## Backend
| Class | Path | Role |
|---|---|---|
| `V2__Seed_data` | `backend/src/main/java/db/migration/V2__Seed_data.java` | Flyway `BaseJavaMigration`; JDBC inserts in dependency order |
| `SeedDtos` | `backend/src/main/java/com/appleetcode/backend/seed/SeedDtos.java` | Jackson records for the seed files |
| `V1__schema.sql` | `backend/src/main/resources/db/migration/` | Tables + initial `app_state` |

**Insert order:**
1. `patterns`
2. `problems`
3. `problem_patterns`
4. `plan_items`, built from core problems sorted by `(pattern.order_in_plan, core_order)`
5. `topics`

## Frontend
—

## Data
- **W (once):** `patterns`, `problems`, `problem_patterns`, `plan_items`, `topics`.

## Invariants & rules
- **Never edit `V1`/`V3` once applied**: Flyway checksums would fail on existing databases. `V2` is a Java migration with no checksum; still, treat it as frozen.
- A missing seed makes `V2` throw an explicit error. The Postgres transaction rolls back, so the next boot retries instead of leaving an empty catalog marked as applied.
- Changing the ETL output does **not** change existing databases (see [etl.md](../etl.md#running-it)).

## Known pitfalls / past bugs
- **A race with Tomcat.** The seed was first an `ApplicationRunner`, which Spring runs *after* the web server starts. A `GET /today` arriving in that window saw an empty plan, concluded that there was nothing to introduce, and permanently switched the phase to `consolidacao`. Moving the seed into Flyway removed the window ([ADR 0002](../decisions/0002-seed-as-flyway-java-migration.md)).

## Tests
- ETL: `tools/etl/test_transform.py`.
- End to end: fresh-volume boot (see [operations.md](../operations.md#running-a-second-isolated-copy)).

## Related
[etl](../etl.md) · [ADR 0002](../decisions/0002-seed-as-flyway-java-migration.md) · [ADR 0007](../decisions/0007-etl-at-build-time-no-data-in-repo.md)
