# Daily time budgets

A personal "chess clock for your day": a few priorities, each with a daily budget, and at most one clock running at a time.

## Language

### Planning

**Priority**:
Something the user wants to spend time on, with a name, a colour, and a weekly budget schedule.
_Avoid_: task, project, category, goal

**Weekly budget schedule**:
A priority's daily budget for each weekday (e.g. Gym 1h Monday to Friday, 0 on weekends).
_Avoid_: plan, routine

**Daily budget**:
The time planned for one priority on one day, taken from its weekly budget schedule.
_Avoid_: goal, quota, limit, allowance

**Total budget**:
The sum of all daily budgets for a day; the full circle of the ring (shown as Σ).
_Avoid_: capacity, day length

**Day**:
A local calendar date, midnight to midnight; budgets, spent time, and remaining time are always per day.
_Avoid_: period, cycle

### Tracking

**Session**:
A continuous stretch during which one priority's clock runs, from start to stop.
_Avoid_: timer, entry, block

**Running**:
The state of the one priority whose session is open; at most one priority is running.
_Avoid_: active, live (UI badge only)

**Start / Stop**:
Opening and closing a session; starting a priority stops the running one. There is no separate paused state.
_Avoid_: pause, resume, suspend

**Adjustment**:
A signed amount of time added to or removed from a priority's spent time on a day without a session.
_Avoid_: log entry, correction, manual entry

**Spent**:
Time attributed to a priority on a day: its sessions plus its adjustments.
_Avoid_: used, tracked, logged

**Remaining**:
Daily budget minus spent, never below zero ("still yours" when totalled across priorities).
_Avoid_: balance, left (UI copy only)

**Overtime**:
Spent time beyond the daily budget.
_Avoid_: overrun, deficit, negative time

### Alerts

**Budget alarm**:
The alarm that rings when a running priority's remaining time reaches zero.
_Avoid_: notification, reminder, timer

**Wrap-up notification**:
An optional heads-up a chosen number of minutes before a running priority's remaining time reaches zero.
_Avoid_: warning, reminder, pre-alarm
