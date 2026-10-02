package com.addedomarus.daybudget.core

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExportTest {
    private val db = DayBudgetDatabase.open(ApplicationProvider.getApplicationContext<Context>(), name = null)
    private val store = EventStore(db.events())
    private val dubai = ZoneId.of("Asia/Dubai")
    private val work = Priority(PriorityId("work"), "Deep work", PaletteColor.Terracotta, wrapUpMinutes = 5)
    private val read = Priority(PriorityId("read"), "Reading", PaletteColor.Mustard)
    private fun at(time: String, day: String) = LocalDateTime.parse("${day}T$time").atZone(dubai).toInstant()

    @After fun close() = db.close()

    private fun query(snapshot: SQLiteDatabase, sql: String): List<List<String>> =
        snapshot.rawQuery(sql, null).use { c ->
            generateSequence { if (c.moveToNext()) c else null }
                .map { r -> List(c.columnCount) { i -> r.getString(i) } }.toList()
        }

    @Test fun exportMatchesTodayAndSplitsSessionsByDay() = runTest {
        store.record(
            listOf(
                Event.PriorityCreated(work), Event.ScheduleSet(work.id, WeeklySchedule.every(60), LocalDate.of(2026, 10, 1)),
                Event.PriorityCreated(read), Event.ScheduleSet(read.id, WeeklySchedule.every(30), LocalDate.of(2026, 10, 1)),
                Event.SessionStarted(work.id),
            ),
            at("08:00", "2026-10-01"),
        )
        store.record(
            listOf(
                Event.SessionStopped(work.id, StopReason.User, at("09:00", "2026-10-01")),
                Event.AdjustmentLogged(read.id, LocalDate.of(2026, 10, 1), 45.minutes, 45.minutes),
            ),
            at("09:00", "2026-10-01"),
        )
        store.record(listOf(Event.SessionStarted(work.id)), at("23:00", "2026-10-02"))
        val now = at("00:30", "2026-10-03")

        val target = File.createTempFile("export", ".db")
        exportSnapshot(db, store.ledgerNow(), target, now, dubai)

        val snapshot = SQLiteDatabase.openDatabase(target.path, null, SQLiteDatabase.OPEN_READONLY)
        assertEquals(
            listOf(
                listOf("2026-10-01", "read", "30", "45", "0", "15"),
                listOf("2026-10-01", "work", "60", "60", "0", "0"),
                listOf("2026-10-02", "read", "30", "0", "30", "0"),
                listOf("2026-10-02", "work", "60", "60", "0", "0"),
                listOf("2026-10-03", "read", "30", "0", "30", "0"),
                listOf("2026-10-03", "work", "60", "30", "30", "0"),
            ),
            query(snapshot, "SELECT day, priority_id, budget_minutes, spent_minutes, remaining_minutes, overtime_minutes FROM daily_totals ORDER BY day, priority_id"),
        )
        assertEquals(
            listOf(
                listOf("work", "2026-10-01", "08:00:00", "09:00:00", "60", "User"),
                listOf("work", "2026-10-02", "23:00:00", "00:00:00", "60", ""),
                listOf("work", "2026-10-03", "00:00:00", "00:30:00", "30", ""),
            ),
            query(snapshot, "SELECT priority_id, day, time(started_at), time(ended_at), minutes, stop_reason FROM sessions ORDER BY started_at"),
        )
        assertEquals("1", query(snapshot, "SELECT value FROM meta WHERE key = 'export_schema_version'").single().single())
        snapshot.close()
        target.delete()
    }
}
