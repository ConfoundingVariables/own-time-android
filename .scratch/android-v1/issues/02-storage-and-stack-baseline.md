# Storage and stack baseline

Type: research
Status: resolved
Blocked by:

## Question

What are the current recommended baselines for a new Kotlin + Jetpack Compose + Material 3 app (Kotlin, AGP, Compose BOM, Material 3 version and adaptive/expressive status, reasonable minSdk given the alarm/notification APIs, targetSdk Play currently requires)? For local storage that is also the user's export format, compare Room and SQLDelight (and raw SQLite) on: exporting a consistent copy of the live database file, schema migrations, an append-only event-log schema, multiplatform reach, and fit with a future Wear OS app syncing over the Wearable Data Layer. What does the Data Layer API need from the data model (device identity, ordering, conflict handling)?

## Answer

Findings: [research/storage-and-stack-baseline.md](../research/storage-and-stack-baseline.md).

- Toolchain: Kotlin 2.4.20, AGP 9.4.0, Gradle 9.6.0, JDK 17, Compose BOM 2026.09.00, Material 3 1.4.0 stable (Expressive still 1.5 alpha), Adaptive 1.3.0. Play requires targetSdk 36; minSdk 26 recommended.
- Storage: Room 2.8.5 lowest-risk; SQLDelight stronger for explicit SQL and KMP. Export via `VACUUM INTO` or the SQLite backup API, never a raw copy of an open WAL database.
- Model: immutable events plus rebuildable projections. Watch sync needs device ids, per-device sequence numbers, idempotent merge, explicit conflict rules. Data Layer `DataClient` persists offline; `MessageClient`/`ChannelClient` do not.
- Other: Hilt is the official DI recommendation (manual DI fine for a small app); Navigation 3 stable; Glance 1.2.0 for later widgets; App Actions still supported.
