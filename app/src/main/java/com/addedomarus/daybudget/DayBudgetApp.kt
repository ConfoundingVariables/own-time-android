package com.addedomarus.daybudget

import android.app.Application
import android.content.Context
import com.addedomarus.daybudget.alerts.Alerts
import com.addedomarus.daybudget.core.DayBudgetDatabase
import com.addedomarus.daybudget.core.Event
import com.addedomarus.daybudget.core.EventStore
import com.addedomarus.daybudget.core.Ledger
import com.addedomarus.daybudget.core.Priority
import com.addedomarus.daybudget.core.PriorityId
import com.addedomarus.daybudget.core.StopReason
import com.addedomarus.daybudget.core.WeeklySchedule
import com.addedomarus.daybudget.core.adjust
import com.addedomarus.daybudget.core.delete
import com.addedomarus.daybudget.core.save
import com.addedomarus.daybudget.core.tap
import com.addedomarus.daybudget.core.unanswered
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@HiltAndroidApp
class DayBudgetApp : Application() {
    @Inject lateinit var alerts: Alerts

    override fun onCreate() {
        super.onCreate()
        File(cacheDir, EXPORT_DIR).deleteRecursively()
        alerts.start()
    }
}

const val EXPORT_DIR = "export"

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton
    fun database(@ApplicationContext context: Context) = DayBudgetDatabase.open(context)

    @Provides @Singleton
    fun store(db: DayBudgetDatabase) = EventStore(db.events())

    @Provides @Singleton
    fun scope() = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}

/** Every user and alarm action, each read-decide-append under one lock so concurrent taps can't interleave. */
@Singleton
class Commands @Inject constructor(private val store: EventStore) {
    private val lock = Mutex()

    private suspend fun record(decide: (Ledger, Instant, ZoneId) -> List<Event>) = lock.withLock {
        val now = Instant.now()
        val zone = ZoneId.systemDefault()
        store.record(decide(store.ledgerNow(), now, zone), now, zone)
    }

    suspend fun tap(id: PriorityId) = record { l, now, _ -> tap(l, id, now) }

    suspend fun save(priority: Priority, schedule: WeeklySchedule) =
        record { l, now, zone -> save(l, priority, schedule, LocalDate.ofInstant(now, zone)) }

    suspend fun adjust(id: PriorityId, day: LocalDate, amount: Duration) =
        record { l, now, zone -> listOf(adjust(l, id, day, amount, now, zone)) }

    suspend fun delete(id: PriorityId) = record { l, now, _ -> delete(l, id, now) }

    suspend fun erase(id: PriorityId) = lock.withLock { store.erase(id) }

    suspend fun stop(id: PriorityId, reason: StopReason) = record { l, now, _ ->
        listOfNotNull(Event.SessionStopped(id, reason, now).takeIf { l.running?.id == id })
    }

    suspend fun keepGoing(id: PriorityId) = record { l, _, _ -> listOfNotNull(Event.KeptGoing(id).takeIf { l.running?.id == id }) }

    /** Ends an unanswered alarm's session at the instant the budget ran out. Returns whether it did. */
    suspend fun endAt(id: PriorityId, ranOut: Instant): Boolean {
        var ended = false
        record { l, _, _ ->
            ended = unanswered(l, id, ranOut)
            listOfNotNull(Event.SessionStopped(id, StopReason.AlarmTimeout, ranOut).takeIf { ended })
        }
        return ended
    }
}
