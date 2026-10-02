package com.addedomarus.daybudget.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.addedomarus.daybudget.R
import com.addedomarus.daybudget.core.PriorityId
import com.addedomarus.daybudget.core.compact
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.launch

/** Three short pages: the ring, the gestures, and allowing alerts. Skippable; shown once. */
@Composable
fun OnboardingScreen(onAllowAlerts: () -> Unit, onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    val pager = rememberPagerState { 3 }
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.SpaceBetween) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            if (pager.currentPage < 2) TextButton(onDone) { Text(stringResource(R.string.skip)) }
        }
        HorizontalPager(pager, Modifier.weight(1f)) { page ->
            when (page) {
                0 -> OnboardingPage(
                    demo = true,
                    title = stringResource(R.string.onboarding_ring_title),
                    body = stringResource(R.string.onboarding_ring_body),
                )
                1 -> OnboardingPage(
                    demo = false,
                    title = stringResource(R.string.onboarding_gestures_title),
                    body = stringResource(R.string.onboarding_gestures_body),
                )
                else -> OnboardingPage(
                    demo = false,
                    title = stringResource(R.string.onboarding_alerts_title),
                    body = stringResource(R.string.onboarding_alerts_body),
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Dots(pager.currentPage)
            Spacer(Modifier.size(8.dp))
            if (pager.currentPage == 2) {
                Button(onAllowAlerts, Modifier.weight(1f)) { Text(stringResource(R.string.allow_alerts)) }
                TextButton(onDone) { Text(stringResource(R.string.start)) }
            } else {
                Button({ scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } }, Modifier.weight(1f)) {
                    Text(stringResource(R.string.next))
                }
            }
        }
    }
}

@Composable
private fun OnboardingPage(demo: Boolean, title: String, body: String) {
    Column(
        Modifier.fillMaxSize().padding(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (demo) DemoRing() else Spacer(Modifier.size(96.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Text(body, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private data class Demo(val name: String, val colour: androidx.compose.ui.graphics.Color, val remaining: Int, val spent: Int)

@Composable
private fun DemoRing() {
    val surface = MaterialTheme.colorScheme.surfaceContainer
    val demo = listOf(
        Demo("Deep work", androidx.compose.ui.graphics.Color(0xFFC8714A), 180, 30),
        Demo("Reading", androidx.compose.ui.graphics.Color(0xFFE0B54E), 132, 24),
        Demo("Gym", androidx.compose.ui.graphics.Color(0xFF7E9670), 90, 18),
        Demo("Family", androidx.compose.ui.graphics.Color(0xFF5F6F82), 72, 36),
    )
    Surface(color = surface, shape = RoundedCornerShape(32.dp), modifier = Modifier.size(260.dp)) {
        Ring(
            rows = demo.map { d ->
                com.addedomarus.daybudget.core.Row(
                    com.addedomarus.daybudget.core.Priority(PriorityId(d.name), d.name, com.addedomarus.daybudget.core.PaletteColor.Slate),
                    (d.remaining + d.spent).minutes,
                    d.spent.minutes,
                    false,
                )
            },
            surface = surface,
            track = MaterialTheme.colorScheme.surfaceContainerHighest,
            indicator = MaterialTheme.colorScheme.onSurface,
            onTap = {},
            onLongPress = {},
            modifier = Modifier.padding(18.dp),
        ) {
            Text(
                (demo.sumOf { it.remaining } ).minutes.compact(),
                style = MaterialTheme.typography.displaySmall.merge(tabular),
            )
            Text(stringResource(R.string.still_yours), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Dots(page: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(3) { i ->
            androidx.compose.foundation.layout.Box(
                Modifier.size(8.dp).background(
                    if (i == page) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                    CircleShape,
                ),
            )
        }
    }
}

@Suppress("unused")
private fun unusedCompact(d: kotlin.time.Duration) = d.compact()
