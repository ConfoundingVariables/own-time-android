package com.addedomarus.daybudget.core

import com.addedomarus.daybudget.core.Alert.BudgetAlarm
import com.addedomarus.daybudget.core.Alert.WrapUpNotice
import com.addedomarus.daybudget.core.Event.AdjustmentLogged
import com.addedomarus.daybudget.core.Event.KeptGoing
import com.addedomarus.daybudget.core.Event.PriorityCreated
import com.addedomarus.daybudget.core.Event.ScheduleSet
import com.addedomarus.daybudget.core.Event.SessionStarted
import com.addedomarus.daybudget.core.Event.SessionStopped
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertEquals
import org.junit.Test

class DomainTest {
    private val dubai = ZoneId.of("Asia/Dubai")
    private val friday = LocalDate.of(2026, 10, 2)
    private val gym = Priority(PriorityId("gym"), "Gym", PaletteColor.Sage)
    private val read = Priority(PriorityId("read"), "Reading", PaletteColor.Mustard)
    private val work = Priority(PriorityId("work"), "Deep work", PaletteColor.Terracotta)

    private fun at(time: String, day: LocalDate = friday): Instant =
        LocalDateTime.of(day, java.time.LocalTime.parse(time)).atZone(dubai).toInstant()

    private val log = ArrayList<Recorded>()
    private fun record(time: String, vararg events: Event, day: LocalDate = friday) {
        events.forEach { log += Recorded(it, at(time, day)) }
    }
    private fun create(p: Priority, minutes: Int, from: LocalDate = friday.minusDays(7)) {
        log += Recorded(PriorityCreated(p), at("00:00", from))
        log += Recorded(ScheduleSet(p.id, WeeklySchedule.every(minutes), from), at("00:00", from))
    }
    private fun ledger() = fold(log)
    private fun todayAt(time: String, day: LocalDate = friday, zone: ZoneId = dubai) = today(ledger(), at(time, day), zone)
    private fun Today.table() = rows.map { listOf(it.priority.name, it.budget, it.spent, it.running) }
    private fun row(name: String, budget: Duration, spent: Duration, running: Boolean = false) = listOf(name, budget, spent, running)

    @Test fun rowsSortByRemainingThenBudgetThenName() {
        create(gym, 60); create(read, 120); create(work, 60)
        create(Priority(PriorityId("a"), "Admin", PaletteColor.Slate), 30)
        record("08:00", SessionStarted(read.id))
        record("09:00", SessionStopped(read.id, StopReason.User, at("09:00")))

        assertEquals(
            listOf(row("Reading", 2.hours, 1.hours), row("Deep work", 1.hours, 0.minutes), row("Gym", 1.hours, 0.minutes), row("Admin", 30.minutes, 0.minutes)),
            todayAt("10:00").table(),
        )
    }

    @Test fun switchingStopsTheRunningPriorityAtTheSameInstant() {
        create(gym, 60); create(read, 60)
        record("09:00", *tap(ledger(), gym.id, at("09:00")).toTypedArray())
        val switch = tap(ledger(), read.id, at("09:30"))
        record("09:30", *switch.toTypedArray())

        assertEquals(listOf(SessionStopped(gym.id, StopReason.Switched, at("09:30")), SessionStarted(read.id)), switch)
        assertEquals(listOf(row("Gym", 1.hours, 30.minutes), row("Reading", 1.hours, 30.minutes, running = true)), todayAt("10:00").table())
        assertEquals(listOf(SessionStopped(read.id, StopReason.User, at("10:00"))), tap(ledger(), read.id, at("10:00")))
    }

    @Test fun overtimeAndNoAlarmOncePastTheBudget() {
        create(gym, 30)
        record("09:00", SessionStarted(gym.id))
        val t = todayAt("09:45")

        assertEquals(listOf(0.minutes, 15.minutes), listOf(t.running!!.remaining, t.running!!.overtime))
        assertEquals(emptyList<Alert>(), t.alerts)
    }

    @Test fun startingASpentPriorityGoesStraightToOvertimeWithoutAlarm() {
        create(gym, 30)
        record("07:00", SessionStarted(gym.id))
        record("07:30", SessionStopped(gym.id, StopReason.User, at("07:30")))
        record("09:00", SessionStarted(gym.id))

        assertEquals(emptyList<Alert>(), todayAt("09:00").alerts)
        assertEquals(10.minutes, todayAt("09:10").running!!.overtime)
    }

    @Test fun alarmAndWrapUpAreDueWhenTheRunningBudgetRunsOut() {
        create(gym.copy(wrapUpMinutes = 5), 60)
        record("09:00", SessionStarted(gym.id))

        assertEquals(listOf(BudgetAlarm(at("10:00"), gym.id), WrapUpNotice(at("09:55"), gym.id)), todayAt("09:10").alerts)
        assertEquals(listOf(BudgetAlarm(at("10:00"), gym.id)), todayAt("09:57").alerts)
        assertEquals(emptyList<Alert>(), todayAt("10:00").alerts)
    }

    @Test fun keepGoingContinuesIntoOvertimeWithoutAnotherAlarm() {
        create(gym, 60)
        record("09:00", SessionStarted(gym.id))
        record("10:00:30", KeptGoing(gym.id))

        val t = todayAt("10:05")
        assertEquals(5.minutes, t.running!!.overtime)
        assertEquals(emptyList<Alert>(), t.alerts)
    }

    @Test fun alarmTimeoutEndsTheSessionWhenTheBudgetRanOut() {
        create(gym, 60)
        record("09:00", SessionStarted(gym.id))
        record("10:01", SessionStopped(gym.id, StopReason.AlarmTimeout, at("10:00")))

        assertEquals(listOf(row("Gym", 1.hours, 1.hours)), todayAt("11:00").table())
    }

    @Test fun anAlarmIsUnansweredUntilStoppedOrKeptGoing() {
        create(gym, 60)
        record("09:00", SessionStarted(gym.id))

        assertEquals(true, unanswered(ledger(), gym.id, at("10:00")))
        record("10:00:20", KeptGoing(gym.id))
        assertEquals(false, unanswered(ledger(), gym.id, at("10:00")))
    }

    @Test fun aBudgetThatRanOutWhileThePhoneWasOffEndsWhenItRanOut() {
        create(gym, 60); create(read, 30)
        record("09:00", SessionStarted(gym.id))

        assertEquals(at("10:00"), missedAlarm(ledger(), at("11:30"), dubai))
        assertEquals(null, missedAlarm(ledger(), at("09:30"), dubai))

        record("12:00", *tap(ledger(), read.id, at("12:00")).toTypedArray())
        record("12:30", *tap(ledger(), read.id, at("12:30")).toTypedArray())
        record("13:00", SessionStarted(read.id))
        assertEquals(null, missedAlarm(ledger(), at("14:00"), dubai))
    }

    @Test fun anAdjustmentThatGivesTimeBackReArmsTheAlarm() {
        create(gym, 60)
        record("09:00", SessionStarted(gym.id))
        record("10:00:30", KeptGoing(gym.id))
        record("10:05", adjust(ledger(), gym.id, friday, (-30).minutes, at("10:05"), dubai))

        assertEquals(listOf(BudgetAlarm(at("10:30"), gym.id)), todayAt("10:05").alerts)
    }

    @Test fun aBudgetEditMidSessionMovesTheAlarm() {
        create(gym, 60)
        record("09:00", SessionStarted(gym.id))
        record("09:10", *save(ledger(), gym, WeeklySchedule.every(90), friday).toTypedArray())

        assertEquals(listOf(BudgetAlarm(at("10:30"), gym.id)), todayAt("09:10").alerts)
    }

    @Test fun aSessionAcrossMidnightCountsTowardEachDay() {
        create(gym, 60)
        record("23:30", SessionStarted(gym.id))
        val saturday = friday.plusDays(1)
        record("00:30", SessionStopped(gym.id, StopReason.User, at("00:30", saturday)), day = saturday)

        assertEquals(30.minutes, ledger().spent(gym.id, friday, dubai, at("01:00", saturday)))
        assertEquals(listOf(row("Gym", 1.hours, 30.minutes)), todayAt("01:00", saturday).table())
    }

    @Test fun theAlarmOfASessionRunningPastMidnightUsesTheNewDaysBudget() {
        create(gym, 60)
        record("23:30", SessionStarted(gym.id))

        assertEquals(listOf(BudgetAlarm(at("01:00", friday.plusDays(1)), gym.id)), todayAt("23:30").alerts)
    }

    @Test fun offDaysHideAPriorityUnlessItRuns() {
        log += Recorded(PriorityCreated(gym), at("00:00"))
        log += Recorded(ScheduleSet(gym.id, WeeklySchedule(listOf(60, 60, 60, 60, 60, 0, 0)), friday), at("00:00"))
        val saturday = friday.plusDays(1)

        assertEquals(listOf(row("Gym", 1.hours, 0.minutes)), todayAt("09:00").table())
        assertEquals(emptyList<Any>(), todayAt("09:00", saturday).table())

        record("09:00", SessionStarted(gym.id), day = saturday)
        assertEquals(listOf(row("Gym", 0.minutes, 10.minutes, running = true)), todayAt("09:10", saturday).table())
        assertEquals(emptyList<Alert>(), todayAt("09:10", saturday).alerts)
    }

    @Test fun scheduleEditsApplyFromTheirDayOnward() {
        create(gym, 60, from = friday.minusDays(1))
        record("12:00", *save(ledger(), gym, WeeklySchedule.every(120), friday).toTypedArray())

        assertEquals(listOf(1.hours, 2.hours, 2.hours), listOf(friday.minusDays(1), friday, friday.plusDays(1)).map { ledger().budget(gym.id, it) })
    }

    @Test fun negativeAdjustmentsStopAtZeroAndPositiveOnesCanCreateOvertime() {
        create(gym, 30)
        record("09:00", SessionStarted(gym.id))
        record("09:20", SessionStopped(gym.id, StopReason.User, at("09:20")))
        val l = ledger()

        assertEquals(AdjustmentLogged(gym.id, friday, (-30).minutes, (-20).minutes), adjust(l, gym.id, friday, (-30).minutes, at("10:00"), dubai))
        assertEquals(AdjustmentLogged(gym.id, friday.minusDays(1), (-15).minutes, 0.minutes), adjust(l, gym.id, friday.minusDays(1), (-15).minutes, at("10:00"), dubai))

        record("10:00", adjust(l, gym.id, friday, 45.minutes, at("10:00"), dubai))
        assertEquals(35.minutes, todayAt("10:00").rows.single().overtime)
    }

    @Test fun adjustingYesterdayLeavesTodayAlone() {
        create(gym, 60)
        record("09:00", adjust(ledger(), gym.id, friday.minusDays(1), 45.minutes, at("09:00"), dubai))

        assertEquals(45.minutes, ledger().spent(gym.id, friday.minusDays(1), dubai, at("09:00")))
        assertEquals(listOf(row("Gym", 1.hours, 0.minutes)), todayAt("09:00").table())
    }

    @Test fun deletingStopsTheRunningPriorityAndHidesItButKeepsItsHistory() {
        create(gym, 60); create(read, 60)
        record("09:00", SessionStarted(gym.id))
        record("09:30", *delete(ledger(), gym.id, at("09:30")).toTypedArray())

        val l = ledger()
        assertEquals(listOf(row("Reading", 1.hours, 0.minutes)), todayAt("10:00").table())
        assertEquals(null, todayAt("10:00").last)
        assertEquals(listOf(Session(gym.id, at("09:00"), at("09:30"), StopReason.User)), l.sessions)
        assertEquals(setOf(gym.id, read.id), l.priorities.keys)
    }

    @Test fun erasedPrioritiesVanishEvenIfTheirEventsAreStillPresent() {
        create(gym, 60)
        record("09:00", SessionStarted(gym.id))
        record("09:30", Event.PriorityErased(gym.id))

        assertEquals(emptyMap<PriorityId, Priority>(), ledger().priorities)
        assertEquals(emptyList<Session>(), ledger().sessions)
    }

    @Test fun lastIsTheMostRecentlyStoppedPriorityForResume() {
        create(gym, 60); create(read, 60)
        record("09:00", SessionStarted(read.id))
        record("09:10", *tap(ledger(), gym.id, at("09:10")).toTypedArray())
        record("09:20", *tap(ledger(), gym.id, at("09:20")).toTypedArray())

        val t = todayAt("10:00")
        assertEquals(null, t.running)
        assertEquals(gym, t.last)
    }

    @Test fun theDayFollowsTheCurrentTimeZone() {
        create(gym, 60)
        val start = Instant.parse("2026-10-02T21:00:00Z")
        log += Recorded(SessionStarted(gym.id), start)
        log += Recorded(SessionStopped(gym.id, StopReason.User, start.plusSeconds(1800)), start.plusSeconds(1800))
        val now = Instant.parse("2026-10-02T21:30:00Z")

        assertEquals(LocalDate.of(2026, 10, 3), today(ledger(), now, dubai).day)
        assertEquals(LocalDate.of(2026, 10, 2), today(ledger(), now, ZoneId.of("Europe/London")).day)
        assertEquals(30.minutes, today(ledger(), now, ZoneId.of("Europe/London")).rows.single().spent)
    }

    @Test fun savingAnUnchangedScheduleOnlyRecordsTheEdit() {
        create(gym, 60)
        assertEquals(listOf(Event.PriorityEdited(gym.copy(name = "Lift"))), save(ledger(), gym.copy(name = "Lift"), WeeklySchedule.every(60), friday))
        assertEquals(emptyList<Event>(), save(ledger(), gym, WeeklySchedule.every(60), friday))
    }

    @Test fun ringDrawsRemainingInOrderThenSpentInReverse() {
        create(gym, 60); create(read, 60); create(work, 120)
        record("08:00", SessionStarted(work.id))
        record("09:00", SessionStopped(work.id, StopReason.User, at("09:00")))
        record("09:00", SessionStarted(gym.id))
        record("09:30", SessionStopped(gym.id, StopReason.User, at("09:30")))

        assertEquals(
            listOf(
                Arc(work.id, 0f, 0.25f, spent = false),
                Arc(read.id, 0.25f, 0.25f, spent = false),
                Arc(gym.id, 0.5f, 0.125f, spent = false),
                Arc(gym.id, 0.625f, 0.125f, spent = true),
                Arc(work.id, 0.75f, 0.25f, spent = true),
            ),
            ring(todayAt("10:00").rows),
        )
    }

    @Test fun compactDurationsRoundUp() {
        val cases = listOf(0.seconds, 30.seconds, 44.minutes + 10.seconds, 59.minutes + 30.seconds, 60.minutes, 90.minutes, 2.hours + 24.minutes, 2.hours + 25.minutes, 8.hours)
        assertEquals(listOf("0m", "1m", "45m", "1h", "1h", "1.5h", "2.4h", "2.5h", "8h"), cases.map { it.compact() })
        assertEquals(listOf("15m", "1h", "1h 15m"), listOf(15.minutes, 60.minutes, 75.minutes).map { it.precise() })
    }
}
