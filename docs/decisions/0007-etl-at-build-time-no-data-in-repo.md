# 0007: Generate the seed at build time; commit no data

**Status:** Accepted (supersedes the original spec's "commit `seed_problems.json`")

## Context
The generated seed (about 1.2 MB derived from third-party datasets) used to be committed. For a public repository we want no redistributed third-party data and no personal data, while keeping builds reproducible.

## Decision
- The ETL (`tools/etl/generate_seed.py`) downloads the sources **pinned to commit SHAs** and writes a git-ignored seed.
- The Dockerfile runs it in a `seed-build` stage; `scripts/seed.sh` runs it for local development.
- ETL tests use synthetic fixtures only.

## Consequences
- The repository contains code and curated mappings only.
- The first Docker build needs internet access. Runtime stays offline (except for the reading feed).
- The same pins produce the same seed; updating a pin is an explicit, reviewable change.
