# Map: Android v1 phone app

Label: wayfinder:map

## Destination

A product spec plus locked stack decisions (ADRs) for the v1 Android phone app, ready to break into tracer-bullet tickets with `/to-tickets`. No production code.

## Notes

- Product: a "chess clock for your day". Priorities with daily budgets; exactly one clock runs at a time; an alarm fires when a budget is spent; everything resets at midnight. Free, Play Store later.
- Working name: "Day Budget", application id `com.addedomarus.daybudget` (placeholder until naming).
- Stack baseline (settled by the user): Kotlin, Jetpack Compose, Material Design 3; full stack in "Stack and architecture".
- UX reference: [reference/observed-ux.md](reference/observed-ux.md). Behaviour and information design are copied faithfully; visual styling follows Material 3.
- Never name or link the iOS app the UX is modelled on (see `AGENTS.md`).
- Watch is a first-class citizen later: every data and sync decision must work for phone-only, watch-only, or both, even though the Wear OS app is a separate future effort. Note from research: the Wearable Data Layer may relay through Google's cloud when Bluetooth is unavailable, so the future watch effort must decide how to word the privacy promise.
- v1 scope: core (priorities and budgets, single running clock, ring, budget alarm, optional wrap-up notification, log past time, midnight reset, SQLite export, light/dark) plus an ongoing session notification. Home-screen widgets, Quick Settings tile, Assistant voice actions, and a user-selectable list-first Today view (prototype variant C) are v1.x: specified, so the design leaves room, but not built in v1.
- Skills: grilling tickets call `grilling` + `domain-modeling`; prototype tickets call `prototype`; research tickets call `research`. Record terms in `GLOSSARY.md`, hard-to-reverse decisions in `docs/adr/`.

## Decisions so far

- Destination: spec plus stack ADRs, not a built app. Wear OS deferred to its own effort but constrains this one. v1 = core + ongoing notification; widgets/QS tile/Assistant are v1.x. Behaviour copied faithfully, visuals Material 3. Name brainstormed later.
- [Android alarm, full-screen alert, and exact-timing rules](issues/01-android-alarm-and-notification-rules.md): exact alarm + plain ongoing notification, no foreground service; full-screen alert needs a user grant on Android 14+; minSdk 26.
- [Storage and stack baseline](issues/02-storage-and-stack-baseline.md): current toolchain pinned; Room (or SQLDelight) with event log + projections; export via VACUUM INTO; sync needs device ids and per-device sequences.
- [Timer semantics](issues/03-timer-semantics.md): alarm offers Stop / Keep going into overtime; spent priorities start straight into overtime; midnight reset (custom day start v1.x); per-weekday budgets in v1, 0-budget priorities hidden from Today; adjustments for today or yesterday.
- [Data model](issues/04-data-model.md): event log + projections in the exported DB (ADR 0001); delete asks Keep history / Erase everything; export adds readable views and a versioned, documented schema.
- [Today screen and ring prototype](issues/05-today-screen-prototype.md): ring-hero layout (B) with tap-to-start chips, long-press for Log/Edit/Delete, Resume card when idle; list-first cards (C) become a post-v1 alternate view.
- [Visual language](issues/06-visual-language.md): full dynamic (wallpaper) colour; curated ~8-colour priority palette harmonized to the scheme; ring surface follows the theme.
- [Stack and architecture](issues/07-stack-and-architecture.md): minSdk 31 (ADR 0002); Kotlin + Compose + Material 3 stable, Room (ADR 0003), Hilt, Navigation Compose, KSP; modules :core + :app; fake-clock unit tests.
- [Alerts and permissions UX](issues/08-alerts-and-permissions-ux.md): first-run onboarding asks for alerts once; alarm rings 1 min then ends the session at budget zero; default alarm sound (custom sound v1.x); USE_EXACT_ALARM with a fallback; plain ongoing notification with Stop.
- [Remaining v1 defaults](issues/09-remaining-v1-defaults.md): empty state, 3-screen onboarding, accessibility baseline, About sheet, export entry points, placeholder name, orb builds + unit tests with device checks on a runner later.

## Not yet specified

Nothing left: the way to the destination is clear.

## Out of scope

- Building the Wear OS app (separate future effort; constraints captured in Notes).
- Statistics or history views inside the app (export is the answer).
- Cloud sync, accounts, servers, analytics.
- iOS.
- v1.x (design leaves room, not built in v1): home-screen widgets, Quick Settings tile, Assistant voice actions, the list-first Today view, a custom alarm sound, and a user-chosen day start.
- Play Store listing: product name, store copy, privacy policy page, licence and whether the code is open source.
