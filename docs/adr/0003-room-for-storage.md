# Room for storage

The event log and its projections (ADR 0001) live in a Room 2.x database. Room is Google's recommended SQLite layer, has the strongest migration tooling (exported schema history, migration tests), returns `Flow` for a live-updating UI, and supports Kotlin Multiplatform if a shared core is ever needed. We considered SQLDelight (explicit SQL, broader multiplatform reach) and raw SQLite (most control, most code) and chose the lowest-risk, least-code option.

## Consequences

- The user export is a consistent snapshot made with `VACUUM INTO` (never a copy of the live WAL file), checked with `PRAGMA integrity_check`, and shared through a `FileProvider`. This path must be proven on a real device early.
- Append-only is enforced in the database (triggers rejecting updates and deletes on the events table, except the erase path), not only by convention in code.
