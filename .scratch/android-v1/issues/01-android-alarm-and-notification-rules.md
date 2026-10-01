# Android alarm, full-screen alert, and exact-timing rules

Type: research
Status: resolved
Blocked by:

## Question

What do current Android versions and Google Play policy allow for an app whose core promise is an alarm that fires the moment a running budget is spent, gets through Do Not Disturb and silent mode, and shows a full-screen alert? Cover exact alarms (SCHEDULE_EXACT_ALARM vs USE_EXACT_ALARM, setAlarmClock), full-screen intents (USE_FULL_SCREEN_INTENT restrictions since Android 14), POST_NOTIFICATIONS, alarm audio streams and DND bypass, Doze and OEM battery killers, ongoing notifications vs foreground services (and which foreground service type, if any, a running timer may use), and lock-screen visibility controls. Note which choices risk Play Store rejection for a free time-budget app.

## Answer

Findings: [research/android-alarm-and-notification-rules.md](../research/android-alarm-and-notification-rules.md).

- Budget expiry: one-shot exact alarm via `setExactAndAllowWhileIdle()` on the monotonic clock; works through Doze but must be rescheduled after reboot, time change, permission revocation, or force-stop.
- Exact-alarm permission: `USE_EXACT_ALARM` is defensible (Play allows core timer apps) but review-dependent; the safe fallback is user-granted `SCHEDULE_EXACT_ALARM`.
- Full-screen alert: since Android 14, `USE_FULL_SCREEN_INTENT` is auto-granted only to alarm/calling apps; others must send the user to settings and degrade to a heads-up notification.
- Alarm sound: `USAGE_ALARM` + `CATEGORY_ALARM` do not unconditionally bypass DND; `POST_NOTIFICATIONS` (Android 13+) is required, alarms not exempt.
- Running session: plain ongoing chronometer notification, no foreground service (no FGS type fits; `specialUse` is a review risk). Android 16 Live Updates optional polish only.
- Hide from lock screen: `VISIBILITY_SECRET`. Recommended minSdk 26.
- Main risks: Play classifying the app as productivity rather than timer; OEM battery killers.
