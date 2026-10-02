package com.addedomarus.daybudget.ui

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import java.time.LocalDate
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

    private fun capture(name: String) {
        compose.setContent { DayBudgetTheme { TodayScreen(TodayUi(LocalDate.of(2026, 10, 2)), onAdd = {}) } }
        compose.onRoot().captureRoboImage("build/screenshots/$name.png")
    }

    @Test fun emptyLight() = capture("today-empty-light")

    @Config(qualifiers = "+night")
    @Test fun emptyDark() = capture("today-empty-dark")
}
