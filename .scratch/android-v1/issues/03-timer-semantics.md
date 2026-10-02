# Timer semantics

Type: grilling
Status: resolved
Blocked by:

## Question

Exactly how do sessions and budgets behave? Settle: pause vs stop (are they the same?); what happens when a budget hits zero (does the session keep running into overtime, stop automatically, or stop when the alarm is dismissed; how is overtime shown); starting another priority while one runs; a session running across midnight; time-zone changes and manual clock changes; budget edits mid-day and mid-session; log adjustments (range, can remaining go above budget or spent go below zero, which day they apply to); what 'Toggle' resumes. Also settle the canonical terms (e.g. 'log entry' vs 'adjustment', 'remaining' vs 'still yours').

## Answer

Terms are fixed in `GLOSSARY.md` (session, start/stop, spent, remaining, overtime, adjustment, weekly budget schedule).

### Decided by the user

- **Budget runs out:** the budget alarm offers **Stop** (default, ends the session) and **Keep going** (the session continues into overtime).
- **Starting a spent priority** is allowed and goes straight into overtime with no alarm.
- **Day boundary:** midnight in v1. A user-chosen day start (e.g. 04:00) is v1.x.
- **Budgets per weekday in v1:** each priority has a weekly budget schedule.
- **Priority with a 0 budget today:** hidden completely from Today; only reachable from the editor.
- **Adjustments** can apply to today or yesterday.

### Mechanical rules (agent-decided, open to veto)

- Start and stop are the only session actions. Starting a priority while another is running stops the running one and starts the new one at the same instant.
- Toggle stops the running priority, or, if none is running, starts the most recently stopped one.
- The budget alarm fires only when a running session crosses from remaining > 0 to 0. It never fires in overtime, never when starting a spent priority, and never again that day for that priority unless an adjustment or budget edit gives it remaining time again.
- **Stop** on the alarm ends the session when tapped. If the alarm times out unanswered, the session ends at the moment remaining reached zero (the user probably wasn't there).
- A session running at midnight continues. Time before midnight counts toward the old day, time after toward the new day's budget. If the new day's budget is 0, the time is overtime and the priority stays visible on Today while running.
- A running priority is always visible on Today, even if hidden by a 0 budget.
- Editing a priority's weekly budget schedule applies from today onward; past days keep the budget they had. Editing mid-session reschedules the alarm; if remaining drops to zero or below because of the edit, the priority enters overtime without an alarm.
- Adjustments: each adjustment is between −4h and +4h. A negative adjustment can't take that day's spent time below zero (clamped). A positive one can create overtime. An adjustment to the running priority reschedules its alarm.
- Durations are measured from elapsed time, not the wall clock, so manual clock changes don't distort sessions. The day an instant belongs to is the local date where the device was at that instant; after a time-zone change, Today recomputes in the new zone.
- No maximum session length.

### Consequences for other tickets

- Today screen prototype: show overtime on the row ("12m over") and in the ring and centre; decide where hidden (0-budget) priorities are managed, since the editor must be reachable without them appearing on Today.
- Data model: store a weekly budget schedule with effective-from dates; adjustments carry a target day; sessions split by day on read.
- Alerts UX: the alarm's Stop / Keep going actions, alarm timeout length.
