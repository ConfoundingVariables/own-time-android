# Alerts and permissions UX

Type: grilling
Status: resolved
Blocked by: 01, 03

## Question

What does the user experience around alarms and notifications look like on Android? Settle: the full-screen alarm screen and its Stop / Keep going actions, and how long it rings before timing out; the ongoing session notification's content and actions (stop, switch?); wrap-up notification timing choices; 'Hide from lock screen' mapping; when and how each permission is requested; what the editor shows when alarms or notifications are denied; behaviour after reboot or app kill mid-session.

## Answer

### Decided by the user

- **Permissions are asked in a short first-run onboarding:** it teaches tap-to-start and long-press for more, then one "Allow alerts" step that requests notification permission and, if Android doesn't grant it automatically, sends the user to the full-screen alarm access settings page.
- **The budget alarm rings for 1 minute.** Unanswered, it becomes a quiet "Budget reached" notification and the session ends at the moment the budget ran out (rule from "Timer semantics").
- **Sound:** the phone's default alarm sound plus vibration, changeable through Android's notification channel settings. A custom app sound is v1.x.

### Mechanics (agent-decided, open to veto)

- **Exact alarm permission:** declare `USE_EXACT_ALARM` (Play allows core timer apps) with truthful store copy positioning the app as a timer. If Play rejects it, switch to `SCHEDULE_EXACT_ALARM` and add an onboarding step for it. Never declare both.
- **Scheduling:** one exact alarm on the elapsed-time clock for the running priority's budget expiry, plus one for its wrap-up notification if enabled. Rescheduled on start, stop, switch, adjustment, budget edit, reboot, time or time-zone change, and app update.
- **Alarm presentation:** a dedicated high-importance alarm notification channel (alarm audio usage, alarm category) with a full-screen intent to the alarm activity: "Time's up · {priority} budget reached", the time, **Stop** (default) and **Keep going**. When the phone is in use, or full-screen access is denied, Android shows it as a heads-up notification with the same two actions.
- **Wrap-up notification:** a separate high-importance (non-alarm) channel; Off / 5 / 10 / 15 minutes before. Only fires while that priority is running.
- **Ongoing session notification:** silent, low-importance, shown only while a session runs: priority name, a countdown chronometer of remaining time (or "{overtime} over"), and a **Stop** action; tapping opens Today. No foreground service. Android 16 Live Updates styling is optional polish.
- **Hide from lock screen:** that priority's ongoing notification is hidden on the lock screen (`VISIBILITY_SECRET`); its alarm still rings but shows "Time's up · budget reached" without the name on the lock screen.
- **Denied or degraded states:** if notifications or full-screen access are off, Today shows a dismissible banner "Alarms are off" with a Fix button to the right settings page, and the editor's alarm line says alarms are off and links there. Checked every time the app comes to the foreground.
- **Reboot or app killed mid-session:** the session stays open (the device being off doesn't stop the clock). On boot the alarms are rescheduled; if the budget ran out while the phone was off, the session ends at the moment it ran out and a "Budget reached" notification is posted (same as an unanswered alarm).
