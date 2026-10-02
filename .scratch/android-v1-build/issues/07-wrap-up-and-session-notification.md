# 07: Wrap-up and ongoing session notification

**What to build:** while a session runs, a silent notification shows the priority with a live countdown and a Stop action (hidden on the lock screen for private priorities), and an optional wrap-up heads-up arrives a few minutes before the budget runs out.

**Blocked by:** 06

**Status:** done

- [x] Editor wrap-up option: Off / 5 / 10 / 15 minutes before; hide-from-lock-screen toggle
- [x] `today` returns the next `WrapUpNotice` for the running priority when enabled and still ahead
- [x] Wrap-up channel (high importance, not alarm) scheduled alongside the budget alarm
- [x] Ongoing low-importance notification with a chronometer countdown (or "{overtime} over") and Stop; tap opens Today; removed when nothing runs; no foreground service
- [x] Hidden priorities: ongoing notification `VISIBILITY_SECRET`; alarm shows no name on the lock screen
- [x] Domain tests: wrap-up instant, none when already inside the window or in overtime
- [x] Device checks listed for the Android runner

## Verification

- Domain tests: wrap-up instant, none when already inside the window or in overtime.

## Defaults I took

- The ongoing notification counts down with a chronometer and switches to counting up at the crossing without a ledger emission.

## On the Android runner

- Ongoing session notification: silent, countdown, Stop action, tap opens Today, disappears when nothing runs.
- A hidden priority: notification is secret on the lock screen; the alarm shows no name there.
- Wrap-up arrives 5/10/15 minutes before the budget runs out, only while running.
