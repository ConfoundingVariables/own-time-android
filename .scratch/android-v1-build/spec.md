# Spec: Day Budget v1 (Android phone app)

Status: ready-for-agent
Source: the resolved wayfinder map in `../android-v1/map.md` and its nine tickets. Terms follow `GLOSSARY.md`; decisions follow `docs/adr/`.

## Problem Statement

I care about a handful of things each day (deep work, the gym, family, a side project), but the day gets eaten by whatever is loudest. To-do lists pile up guilt about things that won't fit today anyway, and time trackers tell me afterwards where my hours went, when it's too late to change. I want to decide in the morning how much of today each priority deserves, see at a glance which ones I'm neglecting right now, and be interrupted by something I can't ignore when I've spent a priority's share. There's no Android app that works like a chess clock for my day.

## Solution

Day Budget gives each priority a daily budget (which can differ by weekday). Only one priority's clock runs at a time: starting one stops the other. A ring shows today's total budget, with every priority's remaining time in bright colour and its spent time in a dark tint, sorted so the priority with the most time left is first. When a running priority's budget is spent, a real alarm rings; I choose Stop or Keep going into overtime. I can log forgotten time or correct over-recorded time for today or yesterday. Everything resets at midnight. My data stays on my phone and I can export the whole history as a SQLite file to analyse however I like.

## User Stories

### Priorities and budgets

1. As a user, I want to create a priority with a name and a colour, so that I can tell my priorities apart at a glance.
2. As a user, I want to choose a priority's colour from a small curated palette of about 8 colours, so that colours stay distinct and look good together.
3. As a user, I want priority colours to blend with my wallpaper-based theme, so that the app feels like part of my phone.
4. As a user, I want to give a priority the same daily budget every day using a slider from 15 minutes to 8 hours, so that setup is quick.
5. As a user, I want one-tap presets of 30 minutes, 1, 2, and 4 hours, so that common budgets take one tap.
6. As a user, I want to switch a priority to per-weekday budgets, so that the gym can have an hour on weekdays and nothing on weekends.
7. As a user, I want to set a weekday to "Off", so that the priority doesn't appear on days I don't plan for it.
8. As a user, I want edits to a budget to apply from today onward, so that past days keep the budget they actually had.
9. As a user, I want to edit a priority's name, colour, and budgets at any time, so that my plan can evolve.
10. As a user, I want the Save button disabled until the priority has a name, so that I can't create a nameless priority.
11. As a user, I want to delete a priority and be asked whether to keep its history or erase everything, so that I control what my export contains.
12. As a user, I want a clear warning that deletion can't be undone, so that I don't lose a priority by accident.
13. As a user, I want a Manage priorities screen listing every priority with its weekly budget schedule, so that I can reach priorities that aren't on Today.
14. As a user, I want priorities with no budget today marked "Not today" in Manage priorities, so that I understand why they're missing from Today.

### Today screen

15. As a user, I want a ring whose full circle is the sum of today's daily budgets, so that I see my planned day, not the 24-hour clock.
16. As a user, I want each priority's remaining time drawn in its colour and its spent time in a dark tint of the same colour, so that I see at a glance what's still mine and what's used.
17. As a user, I want the ring and the chips sorted by remaining time, most first, so that what I'm neglecting is always first.
18. As a user, I want the order to update live as time passes, so that the ring always reflects the current state.
19. As a user, I want the centre of the ring to show the total time still mine when nothing is running, so that I know how much of my planned day is left.
20. As a user, I want the centre to show the running priority's name, its remaining time, and a stop button while a session runs, so that I can follow and stop it from the ring.
21. As a user, I want the running priority's segment highlighted on the ring, so that I can find it instantly.
22. As a user, I want the total budget (Σ) shown under the ring, so that I know the size of today's plan.
23. As a user, I want chips under the ring with each priority's name and remaining time, so that I can read the ring precisely.
24. As a user, I want to tap a chip or a ring segment to start that priority, so that switching takes one tap.
25. As a user, I want starting a priority to stop the running one, so that only one clock runs at a time.
26. As a user, I want to tap the running chip or segment to stop it, so that pausing is as easy as starting.
27. As a user, I want to long-press a chip or segment to get Log time, Edit, and Delete, so that less common actions don't clutter the screen.
28. As a user, I want a bottom card showing the running priority with its remaining time, Log time, and Edit, so that its details are always one glance away.
29. As a user, I want the bottom card to offer "Resume {last priority}" when nothing is running, so that I can continue after a break with one tap.
30. As a user, I want to start a priority whose budget is already spent and go straight into overtime without an alarm, so that I can keep working on something I chose consciously.
31. As a user, I want overtime shown as "+12m" and "12m over", so that I can see I've gone beyond my plan.
32. As a user, I want priorities with no budget today hidden from Today, so that Today shows only what I planned.
33. As a user, I want a running priority to stay visible on Today even if its budget today is zero, so that I can always see and stop what's running.
34. As a user, I want an "Add" chip on Today, so that I can create a priority where I am.
35. As a user, I want Today to show "Nothing planned today" with an "Add your first priority" button when I have no priorities, so that I know how to start.
36. As a user, I want the app bar to show "Today" and the date, so that I know which day I'm looking at.
37. As a user, I want durations shown as minutes under an hour and decimal hours above ("45m", "2.5h"), so that numbers are compact and readable.
38. As a user, I want a countdown never to show 0 before the budget is actually spent, so that I can trust it.

### Sessions, days, and adjustments

39. As a user, I want everything to reset at midnight, so that each day starts fresh.
40. As a user, I want a session running across midnight to keep running and count toward the new day after midnight, so that late work isn't lost or double-counted.
41. As a user, I want to log forgotten time for a priority, so that my day reflects reality.
42. As a user, I want to correct over-recorded time with a negative adjustment, so that a session I forgot to stop doesn't distort my day.
43. As a user, I want to apply an adjustment to today or yesterday, so that I can fix last night's forgotten session in the morning.
44. As a user, I want adjustments between −4h and +4h with presets of ±15m, ±30m, and ±1h, so that typical corrections take one tap.
45. As a user, I want a negative adjustment never to take a day's spent time below zero, so that the numbers stay meaningful.
46. As a user, I want a positive adjustment to be able to push a priority into overtime, so that the record is honest.
47. As a user, I want the day to follow my phone's current time zone, so that travelling doesn't break my day.

### Alarm and notifications

48. As a user, I want an alarm to ring the moment a running priority's budget is spent, so that I notice even when I'm absorbed.
49. As a user, I want the alarm to use my phone's default alarm sound and vibration, so that it's familiar and respects my alarm volume.
50. As a user, I want the alarm to show full screen with "Time's up · {priority} budget reached", so that I can't miss it.
51. As a user, I want the alarm to offer Stop and Keep going, so that I choose whether to end the session or continue into overtime.
52. As a user, I want an unanswered alarm to stop ringing after 1 minute and end the session at the moment the budget ran out, so that time I wasn't there isn't counted.
53. As a user, I want a quiet "Budget reached" notification after an unanswered alarm, so that I know what happened when I come back.
54. As a user, I want the alarm to ring only once per priority per day, so that overtime doesn't keep interrupting me.
55. As a user, I want an optional wrap-up notification 5, 10, or 15 minutes before a running budget runs out, so that I can finish what I'm doing.
56. As a user, I want a silent ongoing notification while a session runs, with the priority, a countdown, and a Stop action, so that I can follow and stop it without opening the app.
57. As a user, I want to hide a priority from the lock screen, so that sensitive priorities stay private.
58. As a user, I want the alarm for a hidden priority to show no name on the lock screen, so that it stays private even when it rings.
59. As a user, I want a banner on Today when alarms can't ring because a permission is off, with a Fix button, so that I don't silently lose the alarm.
60. As a user, I want the editor's alarm line to tell me when alarms are off and link to the right settings, so that I can fix it where I notice it.
61. As a user, I want a running session and its alarm to survive a reboot, so that restarting my phone doesn't break my day.
62. As a user, I want a budget that ran out while my phone was off to end the session at that moment and notify me, so that the record matches an unanswered alarm.

### Onboarding

63. As a new user, I want a short onboarding explaining the ring, so that I understand what I'm looking at.
64. As a new user, I want onboarding to teach tap to start and long-press for more, so that I discover the hidden actions.
65. As a new user, I want onboarding to ask for alert permissions once, with a clear reason, so that I trust why they're needed.
66. As a new user, I want to skip onboarding, so that I can explore on my own.

### Export and privacy

67. As a user, I want to export my complete history as a SQLite file through the Android share sheet, so that I can analyse it with any tool.
68. As a user, I want the export to contain readable tables of priorities, sessions, and per-day totals plus the raw events, so that I can query it without reconstructing anything.
69. As a user, I want the export's schema documented and versioned, so that my queries know what they're reading.
70. As a user, I want the app to need no account, no server, and no analytics, so that my data stays mine.
71. As a user, I want an About sheet stating the privacy promise, what the export contains, the app version, and open-source licences, so that I know what the app does with my data.

### Appearance and accessibility

72. As a user, I want the app to follow my wallpaper colours (Material You), so that it matches my phone.
73. As a user, I want light and dark themes following my system setting, so that the app is comfortable day and night.
74. As a user, I want the ring's background to follow the theme, so that it looks native in both themes.
75. As a TalkBack user, I want each chip and segment described with name, state, and remaining time, so that I can use the app without seeing colours.
76. As a user with large text, I want the app to scale with my font size without clipping, so that I can read it.
77. As a user who prefers reduced motion, I want animations to respect the system setting, so that the app is calm.

## Implementation Decisions

### Modules

- **`:core`**, pure Kotlin plus the Room data layer. Owns the event model, the fold from events to a ledger, the derivation of Today and the next alerts for a given instant and time zone, adjustment clamping, and the export snapshot. No Android UI or system services.
- **`:app`**, Compose UI, view models, Hilt graph, alarm scheduling, notifications, the alarm activity, onboarding, and Android receivers (alarm, boot, time change). Thin shell over `:core`.

### Domain shape (the main seam)

The domain is one deep module with a small surface: append events, and derive Today for an instant. Its core types (sketch, Kotlin):

```kotlin
@JvmInline value class PriorityId(val value: String)

sealed interface Event {               // envelope: deviceId, deviceSeq, at: Instant, zone: ZoneId
  data class PriorityCreated(val id: PriorityId, val name: String, val color: PaletteColor,
                             val schedule: WeeklySchedule, val wrapUp: WrapUp, val hideOnLockScreen: Boolean)
  data class PriorityEdited(val id: PriorityId, val name: String, val color: PaletteColor,
                            val wrapUp: WrapUp, val hideOnLockScreen: Boolean)
  data class ScheduleSet(val id: PriorityId, val schedule: WeeklySchedule, val from: LocalDate)
  data class PriorityDeleted(val id: PriorityId)            // history kept
  data class PriorityErased(val id: PriorityId)             // tombstone; its other events are removed
  data class SessionStarted(val id: PriorityId)
  data class SessionStopped(val reason: StopReason, val endedAt: Instant)
  data class KeptGoing(val id: PriorityId)                  // answered the alarm with Keep going
  data class AdjustmentLogged(val id: PriorityId, val day: LocalDate,
                              val requested: Duration, val applied: Duration)
}
enum class StopReason { User, Switched, AlarmStop, AlarmTimeout }
@JvmInline value class WeeklySchedule(val minutes: List<Int>)  // 7 entries, Monday first, 0 = Off
sealed interface WrapUp { data object Off : WrapUp; data class Before(val minutes: Int) : WrapUp }

data class Row(val priority: Priority, val budget: Duration, val spent: Duration, val running: Boolean) {
  val remaining get() = (budget - spent).coerceAtLeast(ZERO)
  val overtime get() = (spent - budget).coerceAtLeast(ZERO)
}
data class Today(val day: LocalDate, val rows: List<Row>, val last: PriorityId?, val alerts: List<Alert>)
sealed interface Alert { val at: Instant
  data class BudgetAlarm(override val at: Instant, val id: PriorityId) : Alert
  data class WrapUpNotice(override val at: Instant, val id: PriorityId) : Alert
}
```

- `fold(events): Ledger` builds priorities, schedule history, sessions, adjustments, and alarm answers once per change. `today(ledger, now, zone): Today` is cheap and runs every tick. Both pure.
- `rows` is already filtered (budget > 0 today, or running) and sorted (remaining desc, then budget desc, then name). Remaining and overtime are derived, never stored, so they can't disagree with spent.
- At most one open session is structural: starting appends a `SessionStopped(Switched)` for the open one first, in the same transaction.
- `alerts` holds the next budget alarm and wrap-up for the running priority, or nothing. A priority whose remaining was already zero when started, or which has a `KeptGoing` today, gets no alarm.
- Sessions are split at local midnight when computing spent per day. Durations are differences of wall-clock instants.

### Storage

- The live database holds one append-only `events` table (typed nullable columns per event field, unique `(device_id, device_seq)`) and a `meta` table (device id, schema version). Triggers reject updates and deletes except the erase path. (ADR 0001, ADR 0003.)
- No projection tables in the live database: the in-memory ledger from `fold` is the projection.
- Export makes a snapshot with `VACUUM INTO`, then writes readable `priorities`, `sessions`, and `daily_totals` tables into the snapshot from the same `fold`, sets the schema version, runs `PRAGMA integrity_check`, and shares the file through a `FileProvider`. The snapshot is deleted on next launch. The schema is documented in the repository.

### Alerts (from "Alerts and permissions UX")

- `USE_EXACT_ALARM`; one exact alarm per alert in `Today.alerts` on the elapsed-time clock, rescheduled whenever `alerts` changes and after boot, time or time-zone change, and app update.
- Alarm: high-importance alarm channel with alarm audio, full-screen intent to the alarm activity, Stop and Keep going actions, 1-minute timeout that records `SessionStopped(AlarmTimeout, endedAt = alarm instant)` and posts "Budget reached".
- Wrap-up: separate high-importance channel. Ongoing session notification: low-importance, chronometer countdown, Stop action, `VISIBILITY_SECRET` when the priority is hidden from the lock screen. No foreground service.
- Permission state (notifications, full-screen intent, exact alarm) is read on every foreground and drives the "Alarms are off" banner.

### UI

- Screens: onboarding (3 pages), Today, Manage priorities, alarm activity. Editor, log time, and long-press actions are modal bottom sheets; delete is a dialog. Navigation Compose with type-safe routes.
- One view model per screen exposing one immutable UI state, built from the events `Flow` folded to a ledger and combined with a 1-second ticker.
- Material 3 1.4 with dynamic colour; palette colours harmonized toward the scheme's primary. Ring drawn with Compose `Canvas` on a surface-container tone, layout per `../android-v1/issues/05-today-screen-prototype.md`.

### Stack

Kotlin 2.4.20, AGP 9.4.0, Gradle 9.6.0, JDK 17, compile/target SDK 36, min SDK 31 (ADR 0002), Compose BOM 2026.09.00, Material 3 1.4.0, Room 2.8.5 with KSP, Hilt, Navigation Compose 2.10. Application id `com.addedomarus.daybudget`, app name "Day Budget" (placeholder).

## Testing Decisions

- A good test calls the code the way its user does and asserts a literal expected value: given these events, at this instant, in this zone, Today has exactly these rows with these durations, and the next alert is at exactly this instant. No mocks of our own code, no asserting which calls were made.
- **Domain (`:core`, JVM unit tests):** every rule in "Timer semantics" and "Data model" through `fold` + `today`: switching, overtime, starting a spent priority, alarm once per day, Keep going, alarm timeout, midnight split, per-weekday budgets and Off days, schedule edits applying from today, adjustment clamping today and yesterday, deletion vs erase, sorting and ties, time-zone change.
- **Storage (`:core`, Robolectric):** append and read back events through Room; triggers reject updates; erase removes a priority's events and leaves a tombstone; export produces a file that opens, passes `integrity_check`, and whose `daily_totals` match `today` for the same events.
- **Screens (`:app`, Roborazzi on the JVM):** Today idle, running, overtime, empty, and dark theme; editor in both budget modes; log sheet. Screenshots are reviewed images, not pixel-perfect gates.
- **On a device (Android runner, later):** alarm rings and full-screen shows, Stop / Keep going / timeout, wrap-up, ongoing notification, lock-screen hiding, permission flows and banner, reboot mid-session, export through the share sheet.
- No prior art yet: this is a new codebase.

## Out of Scope

- Wear OS app (later effort; the event envelope already carries device id and sequence).
- Home-screen widgets, Quick Settings tile, Assistant voice actions, the list-first Today view, a custom alarm sound, and a user-chosen day start (v1.x).
- Statistics or history screens; cloud sync, accounts, servers, analytics; iOS.
- Play Store listing: final name, store copy, privacy policy page, licence.

## Further Notes

- Never name or link the iOS app this product's UX is modelled on.
- Device-only behaviour can't be verified in the orb (no emulator); keep a list of checks for the Android runner.
