# 0004: Prefix every REST endpoint with `/api`

**Status:** Accepted

## Context
The API and the SPA are served from the same origin. The `/stats` API endpoint collided with the Angular route `/stats`: a refresh on the stats page returned JSON instead of the app.

## Decision
`WebMvcConfig` adds `/api` to every `@RestController`. `SpaController` forwards the SPA routes to `index.html`. The dev proxy forwards `/api` to the backend.

## Consequences
- No collisions, and one proxy rule in development.
- **Every new SPA route must be added to `SpaController`**, or deep links and refreshes return 404.
