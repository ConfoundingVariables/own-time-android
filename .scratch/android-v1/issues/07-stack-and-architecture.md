# Stack and architecture

Type: grilling
Status: resolved
Blocked by: 01, 02

## Question

What stack and code structure do we lock? Settle: minSdk/targetSdk; storage library; module layout (single module vs core/domain modules ready for a Wear OS app); dependency injection; navigation; state management; time source abstraction for testable timers; testing approach. Write ADRs for the hard-to-reverse choices.

## Answer

### Decided by the user

- **minSdk 31 (Android 12+)**, recorded in [ADR 0002](../../../docs/adr/0002-min-sdk-31.md). Supersedes the minSdk 26 recommendation in the research tickets.
- **Stack** (chosen for being the industry standard, well-documented, and least code):
  - Kotlin 2.4.20, AGP 9.4.0, Gradle 9.6.0, JDK 17, compileSdk/targetSdk 36, minSdk 31.
  - Jetpack Compose via BOM 2026.09.00; Material 3 1.4.0 stable. Material 3 Expressive is adopted only once it is stable.
  - Room 2.8.5 for the event log and projections, recorded in [ADR 0003](../../../docs/adr/0003-room-for-storage.md).
  - Hilt for dependency injection, including the alarm and boot broadcast receivers.
  - Navigation Compose 2.10 with type-safe routes (Navigation 3 considered; fewer real-world examples).
  - KSP for Room and Hilt code generation.

### Structure (agent-decided, open to veto)

- **Modules:** `:core` (pure Kotlin domain rules: day attribution, spent / remaining / overtime, sorting, adjustment clamping, alarm crossing; plus the Room data layer and event log) and `:app` (Compose UI, view models, alarm scheduling, notifications, export, Android receivers). A future Wear OS app reuses `:core`.
- **Screens:** Today, Manage priorities, and a full-screen alarm activity. Editor, log time, and long-press actions are modal bottom sheets; delete is a dialog.
- **State:** one view model per screen exposing a single immutable UI state as `StateFlow`, built from Room `Flow`s combined with a ticking clock.
- **Time:** a `Clock` abstraction (wall time, time zone, and elapsed time) injected everywhere, so all timer rules are testable with a fake clock.
- **Tests:** JVM unit tests for every rule in "Timer semantics" and "Data model" against a fake clock, and Room DAO/migration tests. Compose UI and device tests run once an Android runner is connected.
- **Application id:** a placeholder until the product is named (must be final before the first Play upload).
