# 06: Budget alarm

**What to build:** when a running priority's budget is spent, an alarm rings full screen with Stop and Keep going; unanswered for a minute, it ends the session at the moment the budget ran out and leaves a "Budget reached" notification; it survives reboots and clock changes.

**Blocked by:** 03

**Status:** done

- [x] `today` returns the next `BudgetAlarm` for the running priority; none if it was started spent or has `KeptGoing` today
- [x] `USE_EXACT_ALARM`; one exact alarm on the elapsed-time clock, rescheduled whenever the alerts change, and on boot, time or time-zone change, and app update
- [x] Alarm channel (alarm audio usage, alarm category, default alarm sound, vibration) with a full-screen intent to the alarm activity: "Time's up · {priority} budget reached", the time, Stop (default) and Keep going; the same actions on the heads-up notification
- [x] Stop appends `SessionStopped(AlarmStop)`; Keep going appends `KeptGoing`
- [x] After 1 minute unanswered: `SessionStopped(AlarmTimeout, endedAt = alarm instant)` and a quiet "Budget reached" notification
- [x] On boot, a budget that ran out while the phone was off is handled like an unanswered alarm
- [x] Domain tests: next alarm instant, once per day, Keep going, timeout end instant, re-arming after an adjustment or budget edit adds time
- [x] Device checks listed for the Android runner

## Verification

- Domain tests: next alarm instant, none when started spent or already over budget, re-arming after an adjustment or budget edit, timeout end instant, unanswered vs Keep going, a budget that ran out while the phone was off.
- Robolectric cannot ring alarms; receivers and channels are compile- and logic-verified only.

## Defaults I took

- The ringing notification is FLAG_INSISTENT on an alarm-audio channel; the timeout is a second exact alarm, not the activity's lifecycle, so it fires even if the activity is killed.

## On the Android runner

- Alarm rings full screen at budget end with the default alarm sound and vibration.
- Stop ends the session; Keep going continues into overtime with no second alarm.
- Leave it unanswered for a minute: ringing stops, the session ends at the crossing instant, a quiet Budget reached notification appears.
- Reboot mid-session: alarms reschedule; if the budget ran out while off, the session ends then and Budget reached is posted.
- Change the clock or time zone mid-session: alarms reschedule, Today recomputes.
