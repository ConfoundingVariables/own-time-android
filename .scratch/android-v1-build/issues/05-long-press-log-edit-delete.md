# 05: Long-press actions: log time, edit, delete

**What to build:** the user long-presses a chip or segment (or uses the bottom card) to log forgotten time or correct over-recorded time for today or yesterday, edit the priority, or delete it, keeping or erasing its history.

**Blocked by:** 03

**Status:** ready-for-agent

- [ ] Long-press on a chip or segment opens a sheet with Log time, Edit, Delete; the bottom card's Log time and Edit open the same flows
- [ ] Log time sheet: Today / Yesterday, signed slider −4h..+4h in 5-minute steps, presets ±15m/30m/1h, help text
- [ ] `AdjustmentLogged` records requested and applied amounts; a negative amount can't take that day's spent below zero
- [ ] Editing appends `PriorityEdited` (and `ScheduleSet` when budgets change)
- [ ] Delete dialog: "Delete, keep history" appends `PriorityDeleted`; "Erase everything" removes the priority's events and appends `PriorityErased` (the only delete the trigger allows); deleting the running priority stops it first
- [ ] Domain tests: clamping today and yesterday, positive adjustment into overtime, adjustment on the running priority, delete vs erase in `today`
- [ ] Robolectric test: erase removes the events and leaves one tombstone
- [ ] Screenshots: long-press sheet, log sheet, delete dialog
