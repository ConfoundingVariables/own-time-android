# 04: Per-weekday budgets and Manage priorities

**What to build:** the user switches a priority to per-weekday budgets with Off days; on an Off day it disappears from Today and lives in Manage priorities marked "Not today"; days roll over correctly at midnight and across time zones.

**Blocked by:** 02

**Status:** ready-for-agent

- [ ] Editor "Same every day" / "Per weekday" switch; seven rows with ±15m steps and "Off"
- [ ] Saving a changed schedule appends `ScheduleSet` effective from today; past days keep their budget
- [ ] Manage priorities screen (overflow menu): every priority with its weekly schedule and a "Not today" badge; tap opens the editor
- [ ] Day boundary is local midnight in the device's current zone; sessions split at midnight
- [ ] Domain tests: per-weekday budget by day, Off days hidden, schedule edit applies from today, session across midnight, time-zone change
- [ ] Screenshots: editor in per-weekday mode, Manage priorities
