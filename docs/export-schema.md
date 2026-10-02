# Export schema

The export is a standalone SQLite database: a `VACUUM INTO` snapshot of the app's live database plus readable tables derived from the same event fold the app shows. Query it with any SQLite tool.

Schema version: `1`. Read it from `meta`:

```sql
SELECT value FROM meta WHERE key = 'export_schema_version';
```

The readable tables are additive and versioned; their columns may change between major versions. The raw tables below are the app's own storage format and follow [ADR 0001](adr/0001-append-only-event-log-with-projections.md).

## `priorities`

One row per priority ever created (erased priorities have no row).

| column | meaning |
| --- | --- |
| `id` | stable identifier |
| `name` | current name |
| `color` | palette colour name |
| `deleted` | 1 if the user deleted it (history kept) |
| `weekly_schedule` | current schedule: seven minute counts, Monday first, 0 = Off |
| `wrap_up_minutes` | wrap-up notice lead time, 0 = Off |
| `hide_on_lock_screen` | 1 if hidden from the lock screen |

## `sessions`

One row per priority per day; sessions spanning midnight are split at each local midnight boundary. Timestamps are local ISO date-times in the device's time zone at export time; an open session ends at the export moment.

| column | meaning |
| --- | --- |
| `priority_id` | the priority |
| `day` | local date the time counts toward |
| `started_at` / `ended_at` | local start and end of this part |
| `minutes` | whole minutes of this part |
| `stop_reason` | `User`, `Switched`, `AlarmStop`, or `AlarmTimeout`; empty while the session runs |

## `daily_totals`

One row per day and priority, for every day from the first event to the export day, when the priority had a budget or any spent time that day.

| column | meaning |
| --- | --- |
| `day` | local date |
| `priority_id`, `priority_name` | the priority |
| `budget_minutes` | that day's daily budget from the weekly schedule effective on that day |
| `spent_minutes` | sessions plus adjustments that day |
| `remaining_minutes` | budget − spent, floored at 0 |
| `overtime_minutes` | spent − budget, floored at 0 |

## Raw tables

`events` and `meta` are the app's append-only event log. `events` carries the recording envelope (`device_id`, `device_seq`, `at_epoch_ms`, `zone`) plus a `type` (`PriorityCreated`, `PriorityEdited`, `ScheduleSet`, `PriorityDeleted`, `PriorityErased`, `SessionStarted`, `SessionStopped`, `KeptGoing`, `AdjustmentLogged`) and that type's payload columns (`priority_id`, `name`, `color`, `wrap_up_minutes`, `hide_on_lock_screen`, `schedule`, `from_day`, `stop_reason`, `ended_at_epoch_ms`, `day`, `requested_ms`, `applied_ms`). Rows are ordered by `device_seq`; updates and deletes are rejected by database triggers except the erase path. `meta` holds `device_id` and the export schema version.
