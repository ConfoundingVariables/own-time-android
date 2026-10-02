package com.addedomarus.daybudget.core

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.test.core.app.ApplicationProvider
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EventLogTest {
    private val db = DayBudgetDatabase.open(ApplicationProvider.getApplicationContext(), name = null)
    private val store = EventStore(db.events())
    private val dubai = ZoneId.of("Asia/Dubai")
    private val six = Instant.parse("2026-10-02T06:00:00Z")
    private val gym = Priority(PriorityId("gym"), "Gym", PaletteColor.Sage, wrapUpMinutes = 5, hideOnLockScreen = true)
    private val read = Priority(PriorityId("read"), "Reading", PaletteColor.Mustard)

    @After fun close() = db.close()

    @Test fun everyEventTypeReadsBackAsRecorded() = runTest {
        val events = listOf(
            Event.PriorityCreated(gym),
            Event.ScheduleSet(gym.id, WeeklySchedule(listOf(60, 60, 60, 60, 60, 0, 0)), LocalDate.of(2026, 10, 2)),
            Event.PriorityEdited(gym.copy(name = "Gym!")),
            Event.SessionStarted(gym.id),
            Event.KeptGoing(gym.id),
            Event.SessionStopped(gym.id, StopReason.AlarmTimeout, Instant.parse("2026-10-02T05:00:00Z")),
            Event.AdjustmentLogged(gym.id, LocalDate.of(2026, 10, 1), (-30).minutes, (-20).minutes),
            Event.PriorityDeleted(gym.id),
        )
        store.record(events, six, dubai)

        val rows = db.events().all()
        assertEquals(events, rows.map { it.toRecorded().event })
        assertEquals((1L..8L).toList(), rows.map { it.deviceSeq })
        assertEquals(setOf(1_790_920_800_000), rows.map { it.atEpochMs }.toSet())
        assertEquals(setOf("Asia/Dubai"), rows.map { it.zone }.toSet())
    }

    @Test fun deviceIdSurvivesReopening() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val first = DayBudgetDatabase.open(context, "reopen.db").run { events().deviceId().also { close() } }
        val second = DayBudgetDatabase.open(context, "reopen.db").run { events().deviceId().also { close() } }

        assertEquals(first, second)
        assertEquals(36, first.length)
    }

    @Test fun updatesAndDeletesAreRejected() = runTest {
        store.record(listOf(Event.PriorityCreated(gym)), six, dubai)
        val sql = db.openHelper.writableDatabase

        val update = assertThrows(SQLiteConstraintException::class.java) { sql.execSQL("UPDATE events SET name = 'x'") }
        val delete = assertThrows(SQLiteConstraintException::class.java) { sql.execSQL("DELETE FROM events") }

        assertEquals("events are append-only", update.message!!.substringBefore(" ("))
        assertEquals("events are append-only", delete.message!!.substringBefore(" ("))
        assertEquals(listOf("Gym"), db.events().all().map { it.payload.name })
    }

    @Test fun eraseRemovesOnlyThatPrioritysEventsAndLeavesOneTombstone() = runTest {
        store.record(listOf(Event.PriorityCreated(gym), Event.PriorityCreated(read), Event.SessionStarted(gym.id)), six, dubai)

        store.erase(gym.id, six.plusSeconds(60), dubai)

        assertEquals(
            listOf("PriorityCreated" to "read", "PriorityErased" to "gym"),
            db.events().all().map { it.payload.type to it.payload.priorityId },
        )
        val ledger = store.ledger.first()
        assertEquals(listOf(read), ledger.live)
        assertEquals(null, ledger.running)
        assertThrows(SQLiteConstraintException::class.java) { db.openHelper.writableDatabase.execSQL("DELETE FROM events") }
    }
}
