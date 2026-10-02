package com.addedomarus.daybudget.ui

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.addedomarus.daybudget.core.Event
import com.addedomarus.daybudget.core.PaletteColor
import com.addedomarus.daybudget.core.Priority
import com.addedomarus.daybudget.core.PriorityId
import com.addedomarus.daybudget.core.Recorded
import com.addedomarus.daybudget.core.StopReason
import com.addedomarus.daybudget.core.WeeklySchedule
import com.addedomarus.daybudget.core.fold
import com.addedomarus.daybudget.core.today
import com.github.takahirom.roborazzi.captureRoboImage
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class TodayScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private val dubai = ZoneId.of("Asia/Dubai")
    private val work = Priority(PriorityId("work"), "Deep work", PaletteColor.Terracotta)
    private val read = Priority(PriorityId("read"), "Reading", PaletteColor.Mustard)
    private val gym = Priority(PriorityId("gym"), "Gym", PaletteColor.Sage)
    private val family = Priority(PriorityId("family"), "Family", PaletteColor.Slate)

    private fun at(time: String): Instant = LocalDateTime.parse("2026-10-02T$time").atZone(dubai).toInstant()
    private fun priority(p: Priority, minutes: Int) = listOf(
        Recorded(Event.PriorityCreated(p), at("00:00")),
        Recorded(Event.ScheduleSet(p.id, WeeklySchedule.every(minutes), LocalDate.of(2026, 10, 1)), at("00:00")),
    )

    private fun ui(now: String, events: List<Recorded>) = TodayUi(
        today = today(fold(events), at(now), dubai),
        ledger = fold(events),
    )
    private fun onIntent(@Suppress("UNUSED_PARAMETER") i: TodayIntent) {}

    private fun four(now: String, running: Boolean = false) =
        priority(work, 240) + priority(read, 150) + priority(gym, 120) + priority(family, 90) +
            listOf(
                Recorded(Event.SessionStarted(work.id), at("08:00")),
                Recorded(Event.SessionStopped(work.id, StopReason.User, at("08:45")), at("08:45")),
                Recorded(Event.SessionStarted(read.id), at("09:00")),
                Recorded(Event.SessionStopped(read.id, StopReason.User, at("09:24")), at("09:24")),
            ) + if (running) listOf(Recorded(Event.SessionStarted(gym.id), at("10:00"))) else emptyList()

    private fun capture(name: String, ui: TodayUi) {
        compose.setContent { DayBudgetTheme { TodayScreen(ui, ::onIntent) } }
        compose.onRoot().captureRoboImage("build/screenshots/$name.png")
    }

    @Test fun emptyLight() = capture("today-empty-light", ui("10:00", emptyList()))

    @Config(qualifiers = "+night")
    @Test fun emptyDark() = capture("today-empty-dark", ui("10:00", emptyList()))

    @Test fun fourPrioritiesLight() = capture("today-four-light", ui("10:00", four("10:00")))

    @Config(qualifiers = "+night")
    @Test fun fourPrioritiesDark() = capture("today-four-dark", ui("10:00", four("10:00")))

    @Test fun running() = capture("today-running", ui("10:12", four("10:00", running = true)))

    @Config(qualifiers = "+night")
    @Test fun overtime() = capture("today-overtime", ui("12:30", four("10:00", running = true)))

    @Test fun alarmsOffBanner() = capture("today-alarms-off", ui("10:00", four("10:00")).copy(alarmsOff = true))

    @Test fun editor() {
        compose.setContent {
            DayBudgetTheme {
                EditorSheet(work, WeeklySchedule.every(240), alarmsOn = true, onFixAlarms = {}, onSave = { _, _ -> }, onDelete = {}, onDismiss = {})
            }
        }
        compose.onRoot().captureRoboImage("build/screenshots/editor.png")
    }

    @Config(qualifiers = "+night")
    @Test fun editorPerWeekday() {
        val days = WeeklySchedule(listOf(60, 60, 60, 60, 60, 0, 0))
        compose.setContent {
            DayBudgetTheme {
                EditorSheet(gym, days, alarmsOn = true, onFixAlarms = {}, onSave = { _, _ -> }, onDelete = {}, onDismiss = {})
            }
        }
        compose.onRoot().captureRoboImage("build/screenshots/editor-per-weekday.png")
    }

    @Test fun logSheet() {
        compose.setContent {
            DayBudgetTheme {
                LogSheet("Deep work", LocalDate.of(2026, 10, 2), onApply = { _, _ -> }, onDismiss = {})
            }
        }
        compose.onRoot().captureRoboImage("build/screenshots/log-sheet.png")
    }

    @Test fun onboarding() {
        compose.setContent { DayBudgetTheme { OnboardingScreen(onAllowAlerts = {}, onDone = {}) } }
        compose.onRoot().captureRoboImage("build/screenshots/onboarding.png")
    }
}
