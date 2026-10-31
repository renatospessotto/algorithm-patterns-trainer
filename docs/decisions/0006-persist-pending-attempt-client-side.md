# 0006: Persist in-progress attempts in `localStorage`

**Status:** Accepted

## Context
The timer and the result form lived only in component memory. While the user solved on LeetCode in another tab, a page reload, a route change, or the browser discarding the tab reset the card. `submit()` also ignored HTTP errors. In practice, attempts were lost silently for days, and the same two "new" problems stayed on the home screen.

## Decision
- The attempt card stores `{startedAt, elapsedSeconds?}` under `pending-attempt:<problemId>` and restores it on load. The timer counts from `startedAt`.
- The entry is cleared only after a successful POST.
- Failed POSTs show an error and keep the form for a retry.
- Duration is an editable minutes field, and the card offers a manual "already solved" entry.
- `HojeComponent` uses `trackBy`, so reloading the session does not destroy other running cards.

## Consequences
- Progress survives reloads and navigation within the same browser profile (not across devices).
- Storage access is wrapped in try/catch; without storage, the card still works but does not survive a reload.
