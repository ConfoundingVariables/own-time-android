# 06: Budget alarm

**What to build:** when a running priority's budget is spent, an alarm rings full screen with Stop and Keep going; unanswered for a minute, it ends the session at the moment the budget ran out and leaves a "Budget reached" notification; it survives reboots and clock changes.

**Blocked by:** 03

**Status:** ready-for-agent

- [ ] `today` returns the next `BudgetAlarm` for the running priority; none if it was started spent or has `KeptGoing` today
- [ ] `USE_EXACT_ALARM`; one exact alarm on the elapsed-time clock, rescheduled whenever the alerts change, and on boot, time or time-zone change, and app update
- [ ] Alarm channel (alarm audio usage, alarm category, default alarm sound, vibration) with a full-screen intent to the alarm activity: "Time's up · {priority} budget reached", the time, Stop (default) and Keep going; the same actions on the heads-up notification
- [ ] Stop appends `SessionStopped(AlarmStop)`; Keep going appends `KeptGoing`
- [ ] After 1 minute unanswered: `SessionStopped(AlarmTimeout, endedAt = alarm instant)` and a quiet "Budget reached" notification
- [ ] On boot, a budget that ran out while the phone was off is handled like an unanswered alarm
- [ ] Domain tests: next alarm instant, once per day, Keep going, timeout end instant, re-arming after an adjustment or budget edit adds time
- [ ] Device checks listed for the Android runner
