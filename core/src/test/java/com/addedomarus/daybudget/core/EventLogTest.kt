package com.addedomarus.daybudget.core

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.test.core.app.ApplicationProvider
import java.time.Instant
import java.time.ZoneId
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
    private val events = db.events()
    private val dubai = ZoneId.of("Asia/Dubai")

    @After fun close() = db.close()

    @Test fun appendedEventsReadBackInOrderWithASequencePerDevice() = runTest {
        events.append(EventPayload("PriorityDeleted", "p1"), Instant.parse("2026-10-02T06:00:00Z"), dubai)
        events.append(EventPayload("PriorityDeleted", "p2"), Instant.parse("2026-10-02T07:30:00Z"), dubai)

        val device = events.deviceId()
        assertEquals(
            listOf(
                EventRow(device, 1, 1_790_920_800_000, "Asia/Dubai", EventPayload("PriorityDeleted", "p1")),
                EventRow(device, 2, 1_790_926_200_000, "Asia/Dubai", EventPayload("PriorityDeleted", "p2")),
            ),
            events.observe().first(),
        )
        assertEquals(36, device.length)
    }

    @Test fun deviceIdSurvivesReopening() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val first = DayBudgetDatabase.open(context, "reopen.db").run { events().deviceId().also { close() } }
        val second = DayBudgetDatabase.open(context, "reopen.db").run { events().deviceId().also { close() } }

        assertEquals(first, second)
    }

    @Test fun updatesAndDeletesAreRejected() = runTest {
        events.append(EventPayload("PriorityDeleted", "p1"), Instant.parse("2026-10-02T06:00:00Z"), dubai)
        val sql = db.openHelper.writableDatabase

        val update = assertThrows(SQLiteConstraintException::class.java) {
            sql.execSQL("UPDATE events SET priority_id = 'p2'")
        }
        val delete = assertThrows(SQLiteConstraintException::class.java) {
            sql.execSQL("DELETE FROM events")
        }

        assertEquals("events are append-only", update.message!!.substringBefore(" ("))
        assertEquals("events are append-only", delete.message!!.substringBefore(" ("))
        assertEquals(listOf("p1"), events.observe().first().map { it.payload.priorityId })
    }
}
