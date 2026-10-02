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
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.flow.Flow

data class EventPayload(
    val type: String,
    @ColumnInfo(name = "priority_id") val priorityId: String? = null,
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

@Dao
abstract class EventDao {
    @Query("SELECT * FROM events ORDER BY rowid")
    abstract fun observe(): Flow<List<EventRow>>

    @Query("SELECT value FROM meta WHERE key = 'device_id'")
    abstract suspend fun deviceId(): String

    @Query("SELECT COALESCE(MAX(device_seq), 0) + 1 FROM events WHERE device_id = :deviceId")
    protected abstract suspend fun nextSeq(deviceId: String): Long

    @Insert
    protected abstract suspend fun insert(row: EventRow)

    @Transaction
    open suspend fun append(payload: EventPayload, at: Instant, zone: ZoneId) {
        val deviceId = deviceId()
        insert(EventRow(deviceId, nextSeq(deviceId), at.toEpochMilli(), zone.id, payload))
    }
}

@Database(entities = [EventRow::class, MetaRow::class], version = 1)
abstract class DayBudgetDatabase : RoomDatabase() {
    abstract fun events(): EventDao

    companion object {
        /** [name] null opens an in-memory database. */
        fun open(context: Context, name: String? = "day-budget.db"): DayBudgetDatabase =
            (if (name == null) Room.inMemoryDatabaseBuilder(context, DayBudgetDatabase::class.java)
            else Room.databaseBuilder(context, DayBudgetDatabase::class.java, name))
                .addCallback(OnCreate)
                .build()
    }
}

private object OnCreate : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        for (op in listOf("UPDATE", "DELETE")) {
            db.execSQL(
                "CREATE TRIGGER events_no_${op.lowercase()} BEFORE $op ON events " +
                    "BEGIN SELECT RAISE(ABORT, 'events are append-only'); END"
            )
        }
        db.execSQL(
            "INSERT INTO meta (key, value) VALUES ('device_id', ?)",
            arrayOf(UUID.randomUUID().toString()),
        )
    }
}
