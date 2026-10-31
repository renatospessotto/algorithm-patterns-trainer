# Feature: Reading feed (`/leitura`)

## Summary
A general reading list of recent, well-cited **academic papers** per topic (databases, AI, classical ML), fetched from the Semantic Scholar API and cached for 14 days. It is independent of the algorithm-pattern study.

## User flow
1. Open `/leitura`. The page calls `GET /api/papers/feed` for all topics.
2. Pick a topic chip to filter (`?topic=<name>`).
3. Each card links to the paper and shows its venue, year, citations and authors.

## API endpoints
| Method | Path | Purpose |
|---|---|---|
| GET | `/api/papers/feed?topic=` | Cached papers, refreshed when stale ([api.md](../api.md#get-apipapersfeed)) |

## Backend
| Class | Path | Role |
|---|---|---|
| `PapersController` | `backend/.../src/controller/PapersController.java` | Endpoint |
| `PapersService` | `backend/.../src/service/PapersService.java` | Cache check, Semantic Scholar call (`RestClient`), ranking, dedupe, persistence |
| `SemanticScholarPaperDto` | `backend/.../src/service/SemanticScholarPaperDto.java` | External response shape |
| `Topic`, `Paper` + repositories | `src/entity`, `src/repository` | Storage |

Configuration (`application.yml` → `app.papers.*`): `semantic-scholar-base-url` and `cache-ttl-days` (14).

## Frontend
| Component / service | Path | Role |
|---|---|---|
| `LeituraComponent` | `frontend/src/app/features/leitura/` | Topic chips (`TOPIC_OPTIONS`), list |
| `PapersService` | `frontend/src/app/core/services/papers.service.ts` | `getFeed(topic?)` |

## Data
- **R:** `topics`, `papers`.
- **W:** `papers` (the topic's rows are replaced on refresh).

## Refresh algorithm
```
stale = no cached papers for the topic, or newest cached_at older than 14 days
if stale:
    query Semantic Scholar: query=search_query, year=(now-3)-now, limit 20,
                            fields=title,url,year,citationCount,venue,authors
    on error: log a warning and keep the existing cache (even if expired)
    keep the top 10 by citationCount, dedupe by url
    delete the topic's old rows, insert the new ones (skip URLs owned by another topic)
```

## Invariants & rules
- This is the **only runtime network call** in the app; everything else works offline.
- An API failure never clears the cache. The result is empty only if the topic was never fetched successfully.
- "Successful paper" is approximated by citation count among recent papers; there is no open source of "award-winning" papers.

## Known pitfalls / past bugs
- **`TOPIC_OPTIONS` is hard-coded in the frontend** and must match the topic names created by the ETL (`TOPICS` in `tools/etl/generate_seed.py`). Changing one without the other breaks filtering (the API returns 404 for an unknown topic).
- **Semantic Scholar rate-limits anonymous clients.** The cache and the stale fallback absorb it.
- Originally specified as a dev.to articles feed; replaced by papers (see [original spec](../spec/original-spec.md)).

## Tests
—

## Related
[etl](../etl.md) (topics seed)
