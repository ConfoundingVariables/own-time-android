package com.addedomarus.daybudget.core

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.sqlite.db.SupportSQLiteDatabase
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class EventPayload(
    val type: String,
    @ColumnInfo(name = "priority_id") val priorityId: String,
    val name: String? = null,
    val color: String? = null,
    @ColumnInfo(name = "wrap_up_minutes") val wrapUpMinutes: Int? = null,
    @ColumnInfo(name = "hide_on_lock_screen") val hideOnLockScreen: Boolean? = null,
    /** Seven comma-separated minute counts, Monday first. */
    val schedule: String? = null,
    @ColumnInfo(name = "from_day") val fromDay: String? = null,
    @ColumnInfo(name = "stop_reason") val stopReason: String? = null,
    @ColumnInfo(name = "ended_at_epoch_ms") val endedAtEpochMs: Long? = null,
    val day: String? = null,
    @ColumnInfo(name = "requested_ms") val requestedMs: Long? = null,
    @ColumnInfo(name = "applied_ms") val appliedMs: Long? = null,
)

@Entity(tableName = "events", primaryKeys = ["device_id", "device_seq"])
data class EventRow(
    @ColumnInfo(name = "device_id") val deviceId: String,
    @ColumnInfo(name = "device_seq") val deviceSeq: Long,
    @ColumnInfo(name = "at_epoch_ms") val atEpochMs: Long,
    val zone: String,
    @Embedded val payload: EventPayload,
)

@Entity(tableName = "meta")
data class MetaRow(@PrimaryKey val key: String, val value: String)

fun Event.toPayload(): EventPayload {
    val id = id.value
    return when (this) {
        is Event.PriorityCreated -> priority.toPayload("PriorityCreated")
        is Event.PriorityEdited -> priority.toPayload("PriorityEdited")
        is Event.ScheduleSet -> EventPayload("ScheduleSet", id, schedule = schedule.minutes.joinToString(","), fromDay = from.toString())
        is Event.PriorityDeleted -> EventPayload("PriorityDeleted", id)
        is Event.PriorityErased -> EventPayload("PriorityErased", id)
        is Event.SessionStarted -> EventPayload("SessionStarted", id)
        is Event.SessionStopped -> EventPayload("SessionStopped", id, stopReason = reason.name, endedAtEpochMs = endedAt.toEpochMilli())
        is Event.KeptGoing -> EventPayload("KeptGoing", id)
        is Event.AdjustmentLogged -> EventPayload(
            "AdjustmentLogged", id, day = day.toString(),
            requestedMs = requested.inWholeMilliseconds, appliedMs = applied.inWholeMilliseconds,
        )
    }
}

private fun Priority.toPayload(type: String) =
    EventPayload(type, id.value, name, color.name, wrapUpMinutes, hideOnLockScreen)

fun EventRow.toRecorded(): Recorded {
    val p = payload
    val id = PriorityId(p.priorityId)
    fun priority() = Priority(id, p.name!!, PaletteColor.valueOf(p.color!!), p.wrapUpMinutes!!, p.hideOnLockScreen!!)
    val event = when (p.type) {
        "PriorityCreated" -> Event.PriorityCreated(priority())
        "PriorityEdited" -> Event.PriorityEdited(priority())
        "ScheduleSet" -> Event.ScheduleSet(id, WeeklySchedule(p.schedule!!.split(",").map(String::toInt)), LocalDate.parse(p.fromDay))
        "PriorityDeleted" -> Event.PriorityDeleted(id)
        "PriorityErased" -> Event.PriorityErased(id)
        "SessionStarted" -> Event.SessionStarted(id)
        "SessionStopped" -> Event.SessionStopped(id, StopReason.valueOf(p.stopReason!!), Instant.ofEpochMilli(p.endedAtEpochMs!!))
        "KeptGoing" -> Event.KeptGoing(id)
        "AdjustmentLogged" -> Event.AdjustmentLogged(id, LocalDate.parse(p.day), p.requestedMs!!.milliseconds, p.appliedMs!!.milliseconds)
        else -> error("unknown event type ${p.type}")
    }
    return Recorded(event, Instant.ofEpochMilli(atEpochMs))
}

@Dao
abstract class EventDao {
    @Query("SELECT * FROM events ORDER BY rowid")
    abstract fun observe(): Flow<List<EventRow>>

    @Query("SELECT * FROM events ORDER BY rowid")
    abstract suspend fun all(): List<EventRow>

    @Query("SELECT value FROM meta WHERE key = 'device_id'")
    abstract suspend fun deviceId(): String

    @Query("SELECT COALESCE(MAX(device_seq), 0) + 1 FROM events WHERE device_id = :deviceId")
    protected abstract suspend fun nextSeq(deviceId: String): Long

    @Insert
    protected abstract suspend fun insert(row: EventRow)

    @Query("INSERT INTO meta (key, value) VALUES ('erasing', :priorityId)")
    protected abstract suspend fun allowErase(priorityId: String)

    @Query("DELETE FROM meta WHERE key = 'erasing'")
    protected abstract suspend fun disallowErase()

    @Query("DELETE FROM events WHERE priority_id = :priorityId")
    protected abstract suspend fun deleteEvents(priorityId: String)

    @Transaction
    open suspend fun append(payloads: List<EventPayload>, at: Instant, zone: ZoneId) {
        val deviceId = deviceId()
        val first = nextSeq(deviceId)
        payloads.forEachIndexed { i, payload -> insert(EventRow(deviceId, first + i, at.toEpochMilli(), zone.id, payload)) }
    }

    /** The one delete the triggers allow: a priority's own events, replaced by a tombstone. */
    @Transaction
    open suspend fun erase(priorityId: String, at: Instant, zone: ZoneId) {
        allowErase(priorityId)
        deleteEvents(priorityId)
        disallowErase()
        append(listOf(EventPayload("PriorityErased", priorityId)), at, zone)
    }
}

class EventStore(private val dao: EventDao) {
    val ledger: Flow<Ledger> = dao.observe().map { rows -> fold(rows.map { it.toRecorded() }) }

    suspend fun ledgerNow(): Ledger = fold(dao.all().map { it.toRecorded() })

    suspend fun record(events: List<Event>, at: Instant = Instant.now(), zone: ZoneId = ZoneId.systemDefault()) {
        if (events.isNotEmpty()) dao.append(events.map { it.toPayload() }, at, zone)
    }

    suspend fun erase(id: PriorityId, at: Instant = Instant.now(), zone: ZoneId = ZoneId.systemDefault()) =
        dao.erase(id.value, at, zone)
}

@Database(entities = [EventRow::class, MetaRow::class], version = 1)
abstract class DayBudgetDatabase : RoomDatabase() {
    abstract fun events(): EventDao

    companion object {
        const val NAME = "day-budget.db"

        /** [name] null opens an in-memory database. */
        fun open(context: Context, name: String? = NAME): DayBudgetDatabase =
            (if (name == null) Room.inMemoryDatabaseBuilder(context, DayBudgetDatabase::class.java)
            else Room.databaseBuilder(context, DayBudgetDatabase::class.java, name))
                .addCallback(OnCreate)
                .build()
    }
}

private object OnCreate : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TRIGGER events_no_update BEFORE UPDATE ON events BEGIN SELECT RAISE(ABORT, 'events are append-only'); END")
        db.execSQL(
            "CREATE TRIGGER events_no_delete BEFORE DELETE ON events " +
                "WHEN OLD.priority_id IS NOT (SELECT value FROM meta WHERE key = 'erasing') " +
                "BEGIN SELECT RAISE(ABORT, 'events are append-only'); END"
        )
        db.execSQL("INSERT INTO meta (key, value) VALUES ('device_id', ?)", arrayOf(UUID.randomUUID().toString()))
    }
}
