# Observed UX reference

How the reference iOS app behaves, captured from its public website, support and privacy pages, store listing, 10 screenshots, and a 30 s demo video (watched at 2 fps). This is the behaviour v1 copies. Do not name the source app anywhere in this repo.

Items marked _(inferred)_ come from watching the video, not from stated copy; confirm them in the relevant ticket.

## Screens

Phone screenshots (static, from the website):

- [Today, light](screens/phone-overview-light.webp): ring card, Σ total, priority list with one running row.
- [Today, dark](screens/phone-overview-dark.webp): same state in dark theme.
- [New priority](screens/phone-create-priority.webp): colour, name, daily budget, alarm, wrap-up, hide from lock screen.
- [Log entry](screens/phone-log-session.webp): signed adjustment slider and presets.

Frames from the demo video, in order (one session day with a 29m total budget):

1. [Idle](screens/video-01-idle.webp): "28m STILL YOURS", list sorted Deep work 14m, Reading 12m, Stretch 2m.
2. [Deep work running](screens/video-02-running-deep-work.webp): LIVE row, centre shows its name and remaining, running indicator on its segment.
3. [Reading running, re-sorted](screens/video-03-running-reading-resorted.webp): Reading (12m) now has more left than Deep work (11m) and moves to the top of list and ring.
4. [Stretch running near zero](screens/video-04-running-stretch-near-zero.webp): Stretch stays third (least remaining); its tiny segment sits at about 9 o'clock.
5. [Lock-screen session card](screens/video-05-lockscreen-session-card.webp): priority, "0 minutes" remaining, progress bar, Stop.
6. [Budget alarm](screens/video-06-budget-alarm.webp): full-screen "Time's up — Stretch budget reached", slide to stop. (App label masked.)

## Core model

- A handful of **priorities**, each with a name, one of 5 preset colours, and a **daily budget** (15 min to 8 h).
- Budgets behave like mutually exclusive countdown timers: starting one pauses the running one (chess clock).
- At midnight every budget resets.
- Positive framing: the app emphasises time "still yours", not time lost. No to-do items, no statistics.
- The app is an input tool and alarm host. History is an event log on the device, exportable as the SQLite file the app itself runs on.
- No account, no server, no analytics. Phone and watch each work alone and sync peer-to-peer when paired.

## Today screen (phone)

- Large title "Today" with the date beneath. Top-right: export (share) and info buttons.
- A dark card holding the **budget ring**:
  - The full circle is the sum of all daily budgets (Σ shown in the card's corner, e.g. "Σ 7.5h"), not 24 hours.
  - One segment per priority in its colour. Clockwise from 12 o'clock come every priority's remaining part (bright), in list order; continuing round back to 12 come the spent parts (dark tint of the same colour), in reverse order, so each priority's spent part mirrors its remaining part. _(inferred)_
  - Centre, idle: total remaining across all priorities with the caption "STILL YOURS".
  - Centre, running: the running priority's name in its colour, its remaining time, and a pause button in its colour.
  - While running, a white inner arc with an arrowhead and a tick mark highlights the running priority's segment. _(exact geometry to settle in the prototype)_
- **Priorities** list below the card:
  - Each row: colour dot, name, "Xm left · of Yh/day", a play/pause button tinted with the priority colour, and a log (history) button.
  - The running row gets a coloured outline, a ring-style dot, and a "LIVE" badge.
  - The list, and the ring, are sorted by remaining time, largest first; rows reorder live as time is spent. _(inferred: Reading jumped above Deep Work once it had more left)_
- A dashed "Add a priority" button closes the list.
- Tap a row to edit; long-press to delete (with confirmation, no undo). Deletion syncs to other devices.
- Light and dark themes; the ring card stays dark in both.

## Create / edit priority

- Cancel and Save in the top bar; Save disabled until valid.
- Colour: 5 preset swatches (terracotta, mustard, raspberry, sage, slate).
- Name: free text ("e.g. Deep Work").
- Daily budget: a large readout, a slider from 15m to 8h, and presets 30m / 1h / 2h / 4h.
- Alarm: informational, "Always alarms you when this budget runs out."
- Wrap-up notification: optional toggle, a heads-up a set number of minutes before the budget runs out.
- Privacy: "Hide from lock screen" hides this priority's live session display on the lock screen.

## Log entry (adjust past time)

- Opened from a row's history button. Shows the priority, then an "Adjustment" readout (e.g. "+30m").
- A signed slider from −4h to +4h with ticks, plus presets −1h, −30m, −15m, +15m, +30m, +1h.
- Copy: "Positive values add forgotten work to the current period. Negative values correct time that was over-recorded."

## Alerts

- **Alarm (always on):** while a session runs, an alarm fires the moment that priority's budget is spent. It is a real alarm that gets through silent mode and focus modes. Full-screen: "Time's up — {Priority} budget reached", the time, and "slide to stop".
- **Wrap-up notification (optional, per priority):** a time-sensitive notification N minutes before the budget runs out. If the user has revoked alarm permission, the editor offers a wrap-up at 0 minutes so a notification still marks the moment.
- The user can turn alarms off globally only by revoking the OS permission; the editor then says alarms are off and links to system settings.
- Alarm permission is requested the first time a priority is created; notification permission the first time wrap-up is turned on.

## Live session surfaces

- Lock screen live card: priority name in its colour, "Remaining N minutes", a progress bar, and a Stop button. It reached "0 minutes" right before the alarm fired.
- Status-bar island shows remaining time while running.
- Home-screen widgets (small, medium): remaining budget at a glance.

## Voice and automation

Every action is a voice phrase and an automation action: start (named or last priority), stop, toggle (stop if running, else start the last priority), and log past time (asks how many minutes). Toggle can be bound to a hardware button on the watch.

## Watch (later effort, kept for context)

- Ring overview ("4.7h STILL YOURS") and a focused ring for the running priority.
- List with a stop (square) button on the running row; swipe a row to reveal Edit and Log (±).
- Create a priority on the watch: name, hours:minutes picker, colour.
- Delete via swipe › Edit › Delete.
