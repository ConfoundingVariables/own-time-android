package com.addedomarus.daybudget.core

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlin.math.ceil
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

@JvmInline value class PriorityId(val value: String) {
    companion object { fun random() = PriorityId(UUID.randomUUID().toString()) }
}

enum class PaletteColor { Terracotta, Mustard, Raspberry, Sage, Slate, Teal, Plum, Sky }

/** Minutes per weekday, Monday first; 0 is Off. */
@JvmInline value class WeeklySchedule(val minutes: List<Int>) {
    init { require(minutes.size == 7) }
    fun on(day: LocalDate): Duration = minutes[day.dayOfWeek.ordinal].minutes
    val sameEveryDay get() = minutes.distinct().size == 1

    companion object { fun every(minutes: Int) = WeeklySchedule(List(7) { minutes }) }
}

/** [wrapUpMinutes] 0 is Off. */
data class Priority(
    val id: PriorityId,
    val name: String,
    val color: PaletteColor,
    val wrapUpMinutes: Int = 0,
    val hideOnLockScreen: Boolean = false,
)

enum class StopReason { User, Switched, AlarmStop, AlarmTimeout }

sealed interface Event {
    val id: PriorityId

    data class PriorityCreated(val priority: Priority) : Event { override val id get() = priority.id }
    data class PriorityEdited(val priority: Priority) : Event { override val id get() = priority.id }
    data class ScheduleSet(override val id: PriorityId, val schedule: WeeklySchedule, val from: LocalDate) : Event
    data class PriorityDeleted(override val id: PriorityId) : Event
    data class PriorityErased(override val id: PriorityId) : Event
    data class SessionStarted(override val id: PriorityId) : Event
    data class SessionStopped(override val id: PriorityId, val reason: StopReason, val endedAt: Instant) : Event
    data class KeptGoing(override val id: PriorityId) : Event
    data class AdjustmentLogged(
        override val id: PriorityId,
        val day: LocalDate,
        val requested: Duration,
        val applied: Duration,
    ) : Event
}

data class Recorded(val event: Event, val at: Instant)

data class Session(val id: PriorityId, val start: Instant, val end: Instant?, val stopReason: StopReason? = null)
data class Adjustment(val id: PriorityId, val day: LocalDate, val applied: Duration)

data class Ledger(
    /** Every priority not erased, deleted ones included so their history stays readable. */
    val priorities: Map<PriorityId, Priority>,
    val deleted: Set<PriorityId>,
    val schedules: Map<PriorityId, List<Event.ScheduleSet>>,
    val sessions: List<Session>,
    val adjustments: List<Adjustment>,
    val keptGoing: List<Recorded>,
) {
    val live: List<Priority> get() = priorities.values.filter { it.id !in deleted }
    val running: Session? get() = sessions.lastOrNull()?.takeIf { it.end == null }

    fun schedule(id: PriorityId): WeeklySchedule = schedules[id]?.lastOrNull()?.schedule ?: WeeklySchedule.every(0)

    fun budget(id: PriorityId, day: LocalDate): Duration =
        schedules[id]?.lastOrNull { it.from <= day }?.schedule?.on(day) ?: ZERO

    fun spent(id: PriorityId, day: LocalDate, zone: ZoneId, now: Instant): Duration {
        val from = day.atStartOfDay(zone).toInstant()
        val to = day.plusDays(1).atStartOfDay(zone).toInstant()
        return sessions.filter { it.id == id }.sumDurations { overlap(it.start, it.end ?: now, from, to) } +
            adjustments.filter { it.id == id && it.day == day }.sumDurations { it.applied }
    }
}

fun fold(events: List<Recorded>): Ledger {
    val erased = events.mapNotNullTo(HashSet()) { (it.event as? Event.PriorityErased)?.id }
    val priorities = LinkedHashMap<PriorityId, Priority>()
    val deleted = HashSet<PriorityId>()
    val schedules = HashMap<PriorityId, MutableList<Event.ScheduleSet>>()
    val sessions = ArrayList<Session>()
    val adjustments = ArrayList<Adjustment>()
    val keptGoing = ArrayList<Recorded>()

    fun close(at: Instant, reason: StopReason, onlyFor: PriorityId? = null) {
        val open = sessions.lastOrNull()?.takeIf { it.end == null } ?: return
        if (onlyFor != null && open.id != onlyFor) return
        sessions[sessions.lastIndex] = open.copy(end = maxOf(at, open.start), stopReason = reason)
    }

    for (recorded in events) {
        val e = recorded.event
        if (e.id in erased) continue
        when (e) {
            is Event.PriorityCreated -> priorities[e.id] = e.priority
            is Event.PriorityEdited -> if (e.id in priorities) priorities[e.id] = e.priority
            is Event.ScheduleSet -> schedules.getOrPut(e.id) { ArrayList() } += e
            is Event.PriorityDeleted -> { close(recorded.at, StopReason.User, onlyFor = e.id); deleted += e.id }
            is Event.PriorityErased -> Unit
            is Event.SessionStarted -> { close(recorded.at, StopReason.Switched); sessions += Session(e.id, recorded.at, null) }
            is Event.SessionStopped -> close(e.endedAt, e.reason, onlyFor = e.id)
            is Event.KeptGoing -> keptGoing += recorded
            is Event.AdjustmentLogged -> adjustments += Adjustment(e.id, e.day, e.applied)
        }
    }
    return Ledger(priorities, deleted, schedules, sessions, adjustments, keptGoing)
}

data class Row(val priority: Priority, val budget: Duration, val spent: Duration, val running: Boolean) {
    val remaining get() = (budget - spent).coerceAtLeast(ZERO)
    val overtime get() = (spent - budget).coerceAtLeast(ZERO)
}

sealed interface Alert {
    val at: Instant
    val id: PriorityId

    data class BudgetAlarm(override val at: Instant, override val id: PriorityId) : Alert
    data class WrapUpNotice(override val at: Instant, override val id: PriorityId) : Alert
}

data class Today(val day: LocalDate, val rows: List<Row>, val last: Priority?, val alerts: List<Alert>) {
    val running: Row? get() = rows.firstOrNull { it.running }
    val total: Duration get() = rows.sumDurations { it.budget }
    val remaining: Duration get() = rows.sumDurations { it.remaining }
    val budgetAlarm: Alert.BudgetAlarm? get() = alerts.firstNotNullOfOrNull { it as? Alert.BudgetAlarm }
}

private val rowOrder = compareByDescending<Row> { it.remaining }.thenByDescending { it.budget }.thenBy { it.priority.name }

fun today(ledger: Ledger, now: Instant, zone: ZoneId): Today {
    val day = LocalDate.ofInstant(now, zone)
    val running = ledger.running
    val rows = ledger.live
        .map { Row(it, ledger.budget(it.id, day), ledger.spent(it.id, day, zone, now), running?.id == it.id) }
        .filter { it.budget > ZERO || it.running }
        .sortedWith(rowOrder)
    val last = ledger.sessions.lastOrNull { it.id !in ledger.deleted }?.let { ledger.priorities[it.id] }
    return Today(day, rows, last, alerts(ledger, rows.firstOrNull { it.running }, now, zone))
}

/**
 * The budget alarm rings when a running priority's remaining time crosses from above zero to zero, which can
 * happen on a later day if the session runs past midnight into a new budget.
 */
private fun alerts(ledger: Ledger, running: Row?, now: Instant, zone: ZoneId): List<Alert> {
    running ?: return emptyList()
    val id = running.priority.id
    var day = LocalDate.ofInstant(now, zone)
    var from = now
    var remaining = running.remaining
    while (remaining > ZERO) {
        val midnight = day.plusDays(1).atStartOfDay(zone).toInstant()
        val at = from + remaining
        if (at <= midnight) {
            val wrapUp = (at - running.priority.wrapUpMinutes.minutes)
                .takeIf { running.priority.wrapUpMinutes > 0 && it > now }
            return listOfNotNull(Alert.BudgetAlarm(at, id), wrapUp?.let { Alert.WrapUpNotice(it, id) })
        }
        day = day.plusDays(1)
        from = midnight
        remaining = ledger.budget(id, day) - ledger.adjustments.filter { it.id == id && it.day == day }.sumDurations { it.applied }
    }
    return emptyList()
}

/** Events for tapping a priority: start it, stop it if it runs, or switch to it from the running one. */
fun tap(ledger: Ledger, id: PriorityId, now: Instant): List<Event> {
    val running = ledger.running
    return when (running?.id) {
        id -> listOf(Event.SessionStopped(id, StopReason.User, now))
        null -> listOf(Event.SessionStarted(id))
        else -> listOf(Event.SessionStopped(running.id, StopReason.Switched, now), Event.SessionStarted(id))
    }
}

/** A negative adjustment can't take the day's spent time below zero. */
fun adjust(ledger: Ledger, id: PriorityId, day: LocalDate, requested: Duration, now: Instant, zone: ZoneId) =
    Event.AdjustmentLogged(id, day, requested, maxOf(requested, -ledger.spent(id, day, zone, now)))

/** Events for saving the editor: create, or edit only what changed; schedule changes apply from [day]. */
fun save(ledger: Ledger, priority: Priority, schedule: WeeklySchedule, day: LocalDate): List<Event> {
    val existing = ledger.priorities[priority.id]
        ?: return listOf(Event.PriorityCreated(priority), Event.ScheduleSet(priority.id, schedule, day))
    return listOfNotNull(
        Event.PriorityEdited(priority).takeIf { priority != existing },
        Event.ScheduleSet(priority.id, schedule, day).takeIf { schedule != ledger.schedule(priority.id) },
    )
}

/** Deleting keeps history; a running priority is stopped first. */
fun delete(ledger: Ledger, id: PriorityId, now: Instant): List<Event> = listOfNotNull(
    Event.SessionStopped(id, StopReason.User, now).takeIf { ledger.running?.id == id },
    Event.PriorityDeleted(id),
)

/** The alarm at [alarmAt] went unanswered: the session it rang for still runs and nobody chose Keep going. */
fun unanswered(ledger: Ledger, id: PriorityId, alarmAt: Instant): Boolean {
    val running = ledger.running ?: return false
    return running.id == id && running.start < alarmAt &&
        ledger.keptGoing.none { it.event.id == id && it.at >= alarmAt }
}

/**
 * When the running budget ran out while no alarm could ring (the phone was off), the instant it ran out;
 * null if it hasn't, if the priority was started already spent, or if Keep going was chosen.
 */
fun missedAlarm(ledger: Ledger, now: Instant, zone: ZoneId): Instant? {
    val session = ledger.running ?: return null
    val row = today(ledger, now, zone).running ?: return null
    if (row.overtime <= ZERO) return null
    val atStart = today(ledger, session.start, zone).running ?: return null
    if (atStart.remaining <= ZERO || atStart.budget <= ZERO) return null
    val ranOut = maxOf(now - row.overtime, session.start)
    return ranOut.takeIf { unanswered(ledger, session.id, it) }
}

/** Arcs of the ring as fractions of the full circle, clockwise from 12 o'clock. */
data class Arc(val id: PriorityId, val start: Float, val sweep: Float, val spent: Boolean)

/** Remaining parts in row order, then spent parts in reverse order, so each spent part mirrors its remaining part. */
fun ring(rows: List<Row>): List<Arc> {
    val total = rows.sumDurations { it.budget }
    if (total <= ZERO) return emptyList()
    val parts = rows.map { Triple(it.priority.id, it.remaining, false) } +
        rows.reversed().map { Triple(it.priority.id, it.budget - it.remaining, true) }
    var start = 0f
    return parts.filter { it.second > ZERO }.map { (id, length, spent) ->
        val sweep = (length / total).toFloat()
        Arc(id, start, sweep, spent).also { start += sweep }
    }
}

/** "45m" under an hour, "2.5h" from an hour; rounded up so a countdown never shows 0 before it is spent. */
fun Duration.compact(): String {
    val minutes = ceil(inWholeMilliseconds / 60_000.0).toLong()
    if (minutes < 60) return "${minutes}m"
    val tenths = ceil(minutes / 6.0).toLong()
    return if (tenths % 10 == 0L) "${tenths / 10}h" else "${tenths / 10}.${tenths % 10}h"
}

/** "1h 15m", for exact budgets in the editor. */
fun Duration.precise(): String {
    val h = inWholeHours
    val m = inWholeMinutes % 60
    return when {
        h == 0L -> "${m}m"
        m == 0L -> "${h}h"
        else -> "${h}h ${m}m"
    }
}

operator fun Instant.plus(d: Duration): Instant = plusMillis(d.inWholeMilliseconds)
operator fun Instant.minus(d: Duration): Instant = minusMillis(d.inWholeMilliseconds)

private fun overlap(start: Instant, end: Instant, from: Instant, to: Instant): Duration =
    (minOf(end, to).toEpochMilli() - maxOf(start, from).toEpochMilli()).coerceAtLeast(0).milliseconds

inline fun <T> Iterable<T>.sumDurations(f: (T) -> Duration): Duration = fold(ZERO) { acc, t -> acc + f(t) }
