---
description: Create or update a feature doc in docs/features/ from the standard template
argument-hint: <feature-name or path to code>
---

Document the feature **$ARGUMENTS**.

1. Read `docs/features/_template.md`. The output must keep every section, in that order; write "—" for sections that do not apply.
2. Find the feature's code end to end:
   - the endpoint(s) in `backend/src/main/java/com/appleetcode/backend/src/controller/`;
   - the service(s), repositories and entities they use;
   - the matching Angular feature in `frontend/src/app/features/`, its `core/services/*` and any `shared/` components;
   - the tables it reads or writes (`backend/src/main/resources/db/migration/`).
3. If `docs/features/<name>.md` exists, update it in place, keeping still-valid content. Otherwise create it.
4. Describe only what the code does now. Pull "Known pitfalls" from code comments and ADRs, not from guesses.
5. Keep cross-references in sync:
   - endpoint rows in `docs/api.md`;
   - the table in `CLAUDE.md` ("Where to look");
   - the feature table in `README.md`, if user-facing.
6. If the change involved a decision a future reader might reverse, add an ADR in `docs/decisions/` and list it in `docs/decisions/README.md`.
7. Report what changed in the docs and anything in the code that looked inconsistent with them.
