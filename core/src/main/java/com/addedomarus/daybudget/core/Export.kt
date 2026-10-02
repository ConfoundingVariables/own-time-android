package com.addedomarus.daybudget.core

import android.database.sqlite.SQLiteDatabase
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.time.Duration

/** Export snapshot schema, documented in docs/export-schema.md. */
const val EXPORT_SCHEMA_VERSION = 1

/**
 * Copies the live database into [target] with `VACUUM INTO` and adds the readable tables: `priorities`,
 * `sessions` (split by day), and `daily_totals`, written from the same fold the app shows.
 */
suspend fun exportSnapshot(db: DayBudgetDatabase, ledger: Ledger, target: File, now: Instant, zone: ZoneId) {
    target.delete()
    db.openHelper.writableDatabase.execSQL("VACUUM INTO ?", arrayOf<Any>(target.absolutePath))
    val snapshot = SQLiteDatabase.openDatabase(target.path, null, SQLiteDatabase.OPEN_READWRITE)

    val events = db.events().all()
    val firstAt = events.minOfOrNull { it.atEpochMs }
    val firstDay = firstAt?.let { LocalDate.ofInstant(Instant.ofEpochMilli(it), zone) }
    val lastDay = LocalDate.ofInstant(now, zone)

    val priorities = ledger.priorities.values.sortedWith(compareBy { it.name })
    snapshot.execSQL(
        "CREATE TABLE priorities (id TEXT PRIMARY KEY, name TEXT NOT NULL, color TEXT NOT NULL, " +
            "deleted INTEGER NOT NULL, weekly_schedule TEXT NOT NULL, wrap_up_minutes INTEGER NOT NULL, hide_on_lock_screen INTEGER NOT NULL)"
    )
    snapshot.beginTransaction()
    try {
        for (p in priorities) {
            snapshot.execSQL(
                "INSERT INTO priorities VALUES (?,?,?,?,?,?,?)",
                arrayOf<Any>(p.id.value, p.name, p.color.name, if (p.id in ledger.deleted) 1 else 0,
                    ledger.schedule(p.id).minutes.joinToString(","), p.wrapUpMinutes, if (p.hideOnLockScreen) 1 else 0),
            )
        }
        snapshot.setTransactionSuccessful()
    } finally {
        snapshot.endTransaction()
    }

    snapshot.execSQL(
        "CREATE TABLE sessions (priority_id TEXT NOT NULL, day TEXT NOT NULL, started_at TEXT NOT NULL, " +
            "ended_at TEXT, minutes INTEGER NOT NULL, stop_reason TEXT)"
    )
    snapshot.beginTransaction()
    try {
        for (s in ledger.sessions) {
            var day = LocalDate.ofInstant(s.start, zone)
            var cursor = s.start
            val end = s.end ?: now
            while (day.atStartOfDay(zone).toInstant() <= end) {
                val midnight = day.plusDays(1).atStartOfDay(zone).toInstant()
                val minutes = kotlin.math.ceil((minOf(end, midnight).toEpochMilli() - cursor.toEpochMilli()) / 60_000.0).toLong().toInt()
                if (minutes > 0) snapshot.execSQL(
                    "INSERT INTO sessions VALUES (?,?,?,?,?,?)",
                    arrayOf<Any>(s.id.value, day.toString(), cursor.atZone(zone).toLocalDateTime().toString(),
                            minOf(end, midnight).atZone(zone).toLocalDateTime().toString(), minutes, s.stopReason?.name ?: ""),
                )
                cursor = midnight
                day = day.plusDays(1)
            }
        }
        snapshot.setTransactionSuccessful()
    } finally {
        snapshot.endTransaction()
    }

    snapshot.execSQL(
        "CREATE TABLE daily_totals (day TEXT NOT NULL, priority_id TEXT NOT NULL, priority_name TEXT NOT NULL, " +
            "budget_minutes INTEGER NOT NULL, spent_minutes INTEGER NOT NULL, remaining_minutes INTEGER NOT NULL, overtime_minutes INTEGER NOT NULL)"
    )
    snapshot.beginTransaction()
    try {
        if (firstDay != null) for (day in generateSequence(firstDay) { it.plusDays(1) }.takeWhile { it <= lastDay }) {
            for (p in priorities) {
                val row = Row(p, ledger.budget(p.id, day), ledger.spent(p.id, day, zone, now), false)
                if (row.budget <= Duration.ZERO && row.spent <= Duration.ZERO) continue
                snapshot.execSQL(
                    "INSERT INTO daily_totals VALUES (?,?,?,?,?,?,?)",
                    arrayOf<Any>(day.toString(), p.id.value, p.name, row.budget.inWholeMinutes.toInt(),
                        row.spent.inWholeMinutes.toInt(), row.remaining.inWholeMinutes.toInt(), row.overtime.inWholeMinutes.toInt()),
                )
            }
        }
        snapshot.execSQL("INSERT OR REPLACE INTO meta (key, value) VALUES ('export_schema_version', ?)", arrayOf<Any>(EXPORT_SCHEMA_VERSION.toString()))
        snapshot.setTransactionSuccessful()
    } finally {
        snapshot.endTransaction()
    }

    check("ok" == snapshot.rawQuery("PRAGMA integrity_check", null).use { it.moveToFirst(); it.getString(0) }) { "export failed integrity_check" }
    snapshot.close()
}
