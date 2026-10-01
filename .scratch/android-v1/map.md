# Map: Android v1 phone app

Label: wayfinder:map

## Destination

A product spec plus locked stack decisions (ADRs) for the v1 Android phone app, ready to break into tracer-bullet tickets with `/to-tickets`. No production code.

## Notes

- Product: a "chess clock for your day". Priorities with daily budgets; exactly one clock runs at a time; an alarm fires when a budget is spent; everything resets at midnight. Free, Play Store later.
- Stack baseline (settled by the user): Kotlin, Jetpack Compose, Material Design 3.
- UX reference: [reference/observed-ux.md](reference/observed-ux.md). Behaviour and information design are copied faithfully; visual styling follows Material 3.
- Never name or link the iOS app the UX is modelled on (see `AGENTS.md`).
- Watch is a first-class citizen later: every data and sync decision must work for phone-only, watch-only, or both, even though the Wear OS app is a separate future effort.
- v1 scope: core (priorities and budgets, single running clock, ring, budget alarm, optional wrap-up notification, log past time, midnight reset, SQLite export, light/dark) plus an ongoing session notification. Home-screen widgets, Quick Settings tile, and Assistant voice actions are v1.x: specified, so the design leaves room, but not built in v1.
- Skills: grilling tickets call `grilling` + `domain-modeling`; prototype tickets call `prototype`; research tickets call `research`. Record terms in `GLOSSARY.md`, hard-to-reverse decisions in `docs/adr/`.

## Decisions so far

- Destination: spec plus stack ADRs, not a built app. Wear OS deferred to its own effort but constrains this one. v1 = core + ongoing notification; widgets/QS tile/Assistant are v1.x. Behaviour copied faithfully, visuals Material 3. Name brainstormed later.
- [Android alarm, full-screen alert, and exact-timing rules](issues/01-android-alarm-and-notification-rules.md): exact alarm + plain ongoing notification, no foreground service; full-screen alert needs a user grant on Android 14+; minSdk 26.
- [Storage and stack baseline](issues/02-storage-and-stack-baseline.md): current toolchain pinned; Room (or SQLDelight) with event log + projections; export via VACUUM INTO; sync needs device ids and per-device sequences.

## Not yet specified

- Product name and Play Store identity (brainstorm later; doesn't block until listing).
- How v1.x surfaces (Glance widgets, Quick Settings tile, Assistant App Actions / shortcuts) hook into the domain, once the data model and timer rules settle.
- Accessibility: TalkBack reading of the ring, font scaling, colour-blind safety of the priority palette.
- First-run and empty state: what the user sees with zero priorities; when to ask for notification / alarm permissions.
- Info screen contents, privacy policy page, licence and whether the code is open source.
- Testing strategy and verification without an emulator in the orb (Android runner later).

## Out of scope

- Building the Wear OS app (separate future effort; constraints captured in Notes).
- Statistics or history views inside the app (export is the answer).
- Cloud sync, accounts, servers, analytics.
- iOS.
