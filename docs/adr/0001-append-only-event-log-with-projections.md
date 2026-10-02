# Append-only event log with projections, in the exported database

All changes are recorded as immutable events in a single local SQLite database; current state (priorities, sessions, adjustments, budget schedules) is a projection rebuilt from those events, maintained in the same transaction. We chose this over plain state tables because a future watch app must sync peer-to-peer without a server: events with a device id and a per-device sequence number merge idempotently, while mutable rows would need ad hoc conflict handling. The same file is the user's export, with documented read-only views and a schema version on top.

## Consequences

- The one exception to immutability is **erase**: when the user deletes a priority and chooses "Erase everything", its events are physically removed and a tombstone event carrying only the priority id is appended, so other devices erase it too on sync.
- Day attribution is computed on read from each event's UTC instant and recorded time zone, never stored as a derived date.
