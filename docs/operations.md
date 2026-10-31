# Operations

## Run with Docker (daily use)

```bash
cp .env.example .env        # optional; every value has a default
docker compose up --build -d
open http://localhost:8080
```

The build runs the seed ETL, so it needs internet access. Runtime does not, except for the reading feed. On first boot Flyway creates the schema and imports the seed.

| Command | Effect on data |
|---|---|
| `docker compose down` / `up -d` | Safe: data lives in the named volume `pgdata` |
| `docker compose up --build -d` | Safe: rebuilds the app image only |
| `docker compose down -v` | **Deletes all progress** (removes the volume) |

## Environment variables

| Variable | Default | Used by |
|---|---|---|
| `POSTGRES_USER` / `POSTGRES_PASSWORD` / `POSTGRES_DB` | `app_leetcode` | db + app datasource. **Only applied when the volume is first created** |
| `APP_TIMEZONE` | `America/Sao_Paulo` | `AppClock`: when "today" starts; also `TZ` in the container |
| `APP_PORT` | `8080` | Host port for the app |
| `DB_HOST_PORT` | `5435` | Host port for Postgres, bound to `127.0.0.1` only |

The backend also reads the standard `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` and `SPRING_DATASOURCE_PASSWORD` (see `application.yml`).

## Local development

```bash
docker compose up -d db                 # Postgres only
scripts/seed.sh                         # generate the seed once
cd backend && SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5435/app_leetcode mvn spring-boot:run
cd frontend && npm install && npx ng serve   # http://localhost:4200, proxies /api to :8080
```

## Tests

```bash
cd backend && mvn test                        # JUnit: scheduler + planner
cd frontend && npx ng build                   # type-check + production build
python3 -m unittest discover -s tools/etl     # ETL with synthetic fixtures
```

## Running a second, isolated copy

To try a branch without touching your main data, use a different compose project name. It gets its own volume and network:

```bash
APP_PORT=18080 DB_HOST_PORT=15435 docker compose -p scratch up --build -d
docker compose -p scratch down -v        # removes only the scratch volume
```

## Troubleshooting

| Symptom | Cause / fix |
|---|---|
| Boot fails with `Seed não encontrado no classpath` | The seed was not generated. Run `scripts/seed.sh` (local) or build with Docker. The migration rolls back and is retried on the next boot |
| The same problems appear on the home screen day after day | Those problems never got an attempt recorded. The next new problem is "the first plan item without `review_state`". Record them; the card offers "Já resolvi, registrar sem cronômetro" |
| A refresh on a route returns 404 | Add the route to `SpaController` |
| The day does not roll over at midnight | Check `APP_TIMEZONE` |
| The reading feed is empty | Semantic Scholar is unreachable or rate-limited and nothing was cached yet for that topic. It is retried on the next request |
| Inspect the DB | `docker exec -it <db container> psql -U app_leetcode -d app_leetcode` |
