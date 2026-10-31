---
description: Regenerate the local seed with the ETL and summarize the result
---

1. Run `python3 -m unittest discover -s tools/etl`. Stop if it fails.
2. Run `scripts/seed.sh` (it writes the git-ignored `backend/src/main/resources/seed/`).
3. Report the summary the ETL prints: totals, core count, per-pattern counts, unmapped count.
4. Remind the user that an existing database is **not** updated by a new seed (`V2__Seed_data` runs once; see `docs/etl.md`). Never suggest `docker compose down -v` on the default project; suggest an isolated `-p scratch` stack instead.
5. Confirm with `git status --short` that no seed JSON is staged.
