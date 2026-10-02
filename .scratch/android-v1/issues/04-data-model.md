# Data model

Type: grilling
Status: resolved
Blocked by: 02, 03

## Question

What is stored, and in what shape? Settle: append-only event log vs current-state tables (or both, with a projection); the event types; how today's remaining and spent are derived; device identity and ordering so a watch can join later; what the exported SQLite file contains and whether its schema is a documented, stable contract; how priority deletion and edits appear in history.

## Answer

Shape recorded in [ADR 0001](../../../docs/adr/0001-append-only-event-log-with-projections.md): an append-only event log plus projections, in one SQLite database that is also the export.

### Decided by the user

- **Deleting a priority asks each time:** "Keep history" (it disappears from the app, its history stays in the export) or "Erase everything" (its events are removed). No undo.
- **Export friendliness:** the same database, plus built-in readable views (priorities, sessions, per-day totals) and a documented schema with a version number. No promise that views never change across versions.

### Model (agent-decided, open to veto)

- **Event envelope:** device id (random per install), per-device sequence number, UTC instant, time-zone id, event type, priority id. Identity is (device id, sequence), so merging is idempotent.
- **Event types:** priority created, priority edited (name, colour, wrap-up setting, hide-from-lock-screen), weekly budget schedule set (effective from a day), priority deleted (history kept), priority erased (tombstone with id only), session started, session stopped (reason: user, switched, alarm stop, alarm timeout), adjustment logged (target day, requested amount, applied amount after clamping).
- **Projections:** priorities, budget schedule history, sessions (start, end, stop reason), adjustments. Rebuildable from events; updated in the same transaction as the append.
- **Derived on read:** running priority (open session), each day's spent / remaining / overtime per priority, with sessions split at local midnight.
- **Export views:** priorities, sessions (split by day), and daily totals (day, priority, budget, spent, remaining, overtime). Schema version in the database itself; schema documented in the repo.
- **Future watch sync rules (constraints only, built later):** edits to the same field resolve last-writer-wins by instant; delete beats edit; erase removes the priority's events everywhere; two overlapping running sessions from different devices resolve so the later start stops the earlier one at that instant.
