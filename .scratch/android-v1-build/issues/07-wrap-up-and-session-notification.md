# 07: Wrap-up and ongoing session notification

**What to build:** while a session runs, a silent notification shows the priority with a live countdown and a Stop action (hidden on the lock screen for private priorities), and an optional wrap-up heads-up arrives a few minutes before the budget runs out.

**Blocked by:** 06

**Status:** ready-for-agent

- [ ] Editor wrap-up option: Off / 5 / 10 / 15 minutes before; hide-from-lock-screen toggle
- [ ] `today` returns the next `WrapUpNotice` for the running priority when enabled and still ahead
- [ ] Wrap-up channel (high importance, not alarm) scheduled alongside the budget alarm
- [ ] Ongoing low-importance notification with a chronometer countdown (or "{overtime} over") and Stop; tap opens Today; removed when nothing runs; no foreground service
- [ ] Hidden priorities: ongoing notification `VISIBILITY_SECRET`; alarm shows no name on the lock screen
- [ ] Domain tests: wrap-up instant, none when already inside the window or in overtime
- [ ] Device checks listed for the Android runner
