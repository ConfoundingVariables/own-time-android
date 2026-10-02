# 04: Per-weekday budgets and Manage priorities

**What to build:** the user switches a priority to per-weekday budgets with Off days; on an Off day it disappears from Today and lives in Manage priorities marked "Not today"; days roll over correctly at midnight and across time zones.

**Blocked by:** 02

**Status:** done

- [x] Editor "Same every day" / "Per weekday" switch; seven rows with ±15m steps and "Off"
- [x] Saving a changed schedule appends `ScheduleSet` effective from today; past days keep their budget
- [x] Manage priorities screen (overflow menu): every priority with its weekly schedule and a "Not today" badge; tap opens the editor
- [x] Day boundary is local midnight in the device's current zone; sessions split at midnight
- [x] Domain tests: per-weekday budget by day, Off days hidden, schedule edit applies from today, session across midnight, time-zone change
- [x] Screenshots: editor in per-weekday mode, Manage priorities

## Verification

- Domain tests: per-weekday budgets by day, Off days hidden, a running Off-day priority stays visible, schedule edits apply from their day, midnight split, time-zone change.
- Roborazzi: `editor-per-weekday`.

## Defaults I took

- The per-weekday editor uses one 0–8h slider per day (0 = Off); no stepper.

## On the Android runner

- Check that a session running across midnight counts toward the new day and that Manage priorities lists Off-day ones with a Not today badge.
