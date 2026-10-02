# Append-only event log, folded in memory

All changes are recorded as immutable events in a single local SQLite database. Current state (priorities, budget schedules, sessions, adjustments) is a projection folded from those events in memory by one pure function; the database stores no projection tables. We chose an event log over plain state tables because a future watch app must sync peer-to-peer without a server: events with a device id and a per-device sequence number merge idempotently, while mutable rows would need ad hoc conflict handling. We chose an in-memory fold over projection tables because a personal day planner produces a few dozen events a day, and one fold in Kotlin is a single source of truth that SQL projections would duplicate.

## Consequences

- The export is a `VACUUM INTO` snapshot of the live database to which readable `priorities`, `sessions`, and `daily_totals` tables are added from the same fold, plus a schema version.
- The one exception to immutability is **erase**: when the user deletes a priority and chooses "Erase everything", its events are physically removed and a tombstone event carrying only the priority id is appended, so other devices erase it too on sync.
- Day attribution is computed on read from each event's UTC instant and recorded time zone, never stored as a derived date.
