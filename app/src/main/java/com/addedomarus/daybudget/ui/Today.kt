package com.addedomarus.daybudget.ui

import android.text.format.DateFormat
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.addedomarus.daybudget.Commands
import com.addedomarus.daybudget.Exporter
import com.addedomarus.daybudget.R
import com.addedomarus.daybudget.alerts.Alerts
import com.addedomarus.daybudget.core.EventStore
import com.addedomarus.daybudget.core.Ledger
import com.addedomarus.daybudget.core.Priority
import com.addedomarus.daybudget.core.PriorityId
import com.addedomarus.daybudget.core.Today
import com.addedomarus.daybudget.core.WeeklySchedule
import com.addedomarus.daybudget.core.compact
import com.addedomarus.daybudget.core.today
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import android.content.Intent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TodayUi(
    val today: Today,
    val ledger: Ledger,
    val alarmsOff: Boolean = false,
)

sealed interface TodayIntent {
    data class Tap(val id: PriorityId) : TodayIntent
    data class Hold(val id: PriorityId) : TodayIntent
    data class Log(val id: PriorityId) : TodayIntent
    data class Edit(val id: PriorityId) : TodayIntent
    data object Add : TodayIntent
    data object Manage : TodayIntent
    data object Export : TodayIntent
    data object About : TodayIntent
    data object FixAlarms : TodayIntent
    data object DismissBanner : TodayIntent
}

private val ticker = flow {
    while (true) {
        val now = Instant.now()
        emit(now)
        delay(1_000 - now.toEpochMilli() % 1_000)
    }
}

/** Shared by Today and Manage priorities: both read the same ledger and edit priorities through the same sheets. */
@HiltViewModel
class MainViewModel @Inject constructor(
    store: EventStore,
    private val commands: Commands,
    private val alerts: Alerts,
    private val exporter: Exporter,
) : ViewModel() {
    private val access = MutableStateFlow(alerts.access())
    private val bannerDismissed = MutableStateFlow(false)

    val state = combine(store.ledger, ticker, access, bannerDismissed) { ledger, now, access, dismissed ->
        TodayUi(today(ledger, now, ZoneId.systemDefault()), ledger, alarmsOff = !access.alarmsOn && !dismissed)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val alarmsOn get() = access.value.alarmsOn

    fun refreshAccess() { access.value = alerts.access() }
    fun dismissBanner() { bannerDismissed.value = true }
    fun fixIntent() = alerts.fixIntent()

    fun tap(id: PriorityId) = viewModelScope.launch { commands.tap(id) }
    fun save(priority: Priority, schedule: WeeklySchedule) = viewModelScope.launch { commands.save(priority, schedule) }
    fun adjust(id: PriorityId, day: LocalDate, amount: Duration) = viewModelScope.launch { commands.adjust(id, day, amount) }
    fun delete(id: PriorityId) = viewModelScope.launch { commands.delete(id) }
    fun erase(id: PriorityId) = viewModelScope.launch { commands.erase(id) }
    suspend fun export(): File = exporter.export()
    fun share(file: File): Intent = exporter.share(file)
}

val tabular = TextStyle(fontFeatureSettings = "tnum")

@Composable
fun dateLabel(day: LocalDate): String {
    val locale = Locale.getDefault()
    return day.format(DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "EEEEdMMMM"), locale))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(ui: TodayUi, onIntent: (TodayIntent) -> Unit) {
    val today = ui.today
    var menu by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.today))
                        Text(dateLabel(today.day), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton({ onIntent(TodayIntent.Export) }) { Icon(Icons.Default.Share, stringResource(R.string.export_history)) }
                    Box {
                        IconButton({ menu = true }) { Icon(Icons.Default.MoreVert, stringResource(R.string.more)) }
                        DropdownMenu(menu, { menu = false }) {
                            DropdownMenuItem({ Text(stringResource(R.string.manage_priorities)) }, { menu = false; onIntent(TodayIntent.Manage) })
                            DropdownMenuItem({ Text(stringResource(R.string.export_history)) }, { menu = false; onIntent(TodayIntent.Export) })
                            DropdownMenuItem({ Text(stringResource(R.string.about)) }, { menu = false; onIntent(TodayIntent.About) })
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (ui.alarmsOff) AlarmsOffBanner(onIntent)
            RingCard(ui, onIntent)
            if (today.rows.isNotEmpty()) {
                Text(
                    stringResource(R.string.ring_caption, today.total.compact()),
                    Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Chips(today, onIntent)
            }
            BottomCard(today, onIntent)
            Spacer(Modifier.size(8.dp))
        }
    }
}

@Composable
private fun AlarmsOffBanner(onIntent: (TodayIntent) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.onErrorContainer)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(stringResource(R.string.alarms_off), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onErrorContainer)
                Text(stringResource(R.string.alarms_off_text), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
            }
            TextButton({ onIntent(TodayIntent.FixAlarms) }) { Text(stringResource(R.string.fix)) }
            IconButton({ onIntent(TodayIntent.DismissBanner) }) { Icon(Icons.Default.Close, stringResource(R.string.dismiss)) }
        }
    }
}

@Composable
private fun RingCard(ui: TodayUi, onIntent: (TodayIntent) -> Unit) {
    val today = ui.today
    val surface = MaterialTheme.colorScheme.surfaceContainer
    val running = today.running
    val summary = when {
        running != null -> describe(running)
        today.rows.isEmpty() -> stringResource(R.string.nothing_planned)
        else -> stringResource(R.string.still_yours_summary, today.remaining.compact(), today.total.compact())
    }
    Surface(color = surface, shape = RoundedCornerShape(32.dp)) {
        Ring(
            rows = today.rows,
            surface = surface,
            track = MaterialTheme.colorScheme.surfaceContainerHighest,
            indicator = MaterialTheme.colorScheme.onSurface,
            onTap = { onIntent(TodayIntent.Tap(it)) },
            onLongPress = { onIntent(TodayIntent.Hold(it)) },
            modifier = Modifier.padding(20.dp).semantics { contentDescription = summary },
        ) {
            Column(Modifier.fillMaxWidth(0.62f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                when {
                    running != null -> {
                        val colour = running.priority.color.color()
                        Text(
                            running.priority.name.uppercase(),
                            style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 1.5.sp, fontWeight = FontWeight.SemiBold),
                            color = colour, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                        )
                        val over = running.overtime > ZERO
                        Text(
                            if (over) "+${running.overtime.compact()}" else running.remaining.compact(),
                            style = MaterialTheme.typography.displayMedium.merge(tabular),
                        )
                        if (over) Text(stringResource(R.string.overtime), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FilledIconButton(
                            { onIntent(TodayIntent.Tap(running.priority.id)) },
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = colour),
                            modifier = Modifier.size(56.dp),
                        ) {
                            Box(Modifier.size(16.dp).background(colour.onColor(), RoundedCornerShape(3.dp)).semantics { contentDescription = "Stop" })
                        }
                    }
                    today.rows.isEmpty() -> {
                        Text(stringResource(R.string.nothing_planned), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                        Spacer(Modifier.size(8.dp))
                        Button({ onIntent(TodayIntent.Add) }) { Text(stringResource(R.string.add_first_priority)) }
                        if (ui.ledger.live.isNotEmpty()) TextButton({ onIntent(TodayIntent.Manage) }) { Text(stringResource(R.string.manage_priorities)) }
                    }
                    else -> {
                        Text(today.remaining.compact(), style = MaterialTheme.typography.displayLarge.merge(tabular))
                        Text(stringResource(R.string.still_yours), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun describe(row: com.addedomarus.daybudget.core.Row): String = when {
    row.overtime > ZERO -> stringResource(R.string.describe_over, row.priority.name, if (row.running) stringResource(R.string.running) else stringResource(R.string.stopped), row.overtime.compact(), row.budget.compact())
    else -> stringResource(R.string.describe_left, row.priority.name, if (row.running) stringResource(R.string.running) else stringResource(R.string.stopped), row.remaining.compact(), row.budget.compact())
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Chips(today: Today, onIntent: (TodayIntent) -> Unit) {
    FlowRow(
        Modifier.fillMaxWidth().animateContentSize(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (row in today.rows) {
            val colour = row.priority.color.color()
            val shape = RoundedCornerShape(50)
            val content = if (row.running) colour.onColor() else MaterialTheme.colorScheme.onSurface
            val description = describe(row)
            Row(
                Modifier
                    .clip(shape)
                    .background(if (row.running) colour else MaterialTheme.colorScheme.surfaceContainerLow)
                    .border(BorderStroke(1.dp, if (row.running) colour else MaterialTheme.colorScheme.outlineVariant), shape)
                    .combinedClickable(onClick = { onIntent(TodayIntent.Tap(row.priority.id)) }, onLongClick = { onIntent(TodayIntent.Hold(row.priority.id)) })
                    .semantics(mergeDescendants = true) { contentDescription = description }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(Modifier.size(10.dp).background(if (row.running) content else colour, CircleShape))
                Text(row.priority.name, style = MaterialTheme.typography.labelLarge, color = content)
                Text(
                    if (row.overtime > ZERO) "+${row.overtime.compact()}" else row.remaining.compact(),
                    style = MaterialTheme.typography.labelLarge.merge(tabular),
                    color = content.copy(alpha = 0.75f),
                )
            }
        }
        Row(
            Modifier
                .clip(RoundedCornerShape(50))
                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), RoundedCornerShape(50))
                .combinedClickable(onClick = { onIntent(TodayIntent.Add) })
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Add, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.add), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun BottomCard(today: Today, onIntent: (TodayIntent) -> Unit) {
    val running = today.running
    val subject = running
        ?: today.last?.let { last -> today.rows.firstOrNull { it.priority.id == last.id } ?: com.addedomarus.daybudget.core.Row(last, ZERO, ZERO, false) }
        ?: return
    val p = subject.priority
    val colour = p.color.color()
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(12.dp).background(colour, CircleShape))
                Text(p.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f, fill = false))
                if (running != null) {
                    Text(
                        stringResource(R.string.live),
                        Modifier.background(colour.copy(alpha = 0.18f), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = colour,
                    )
                }
            }
            Text(
                when {
                    subject.overtime > ZERO -> stringResource(R.string.over, subject.overtime.compact())
                    running != null -> stringResource(R.string.left_of_today, subject.remaining.compact(), subject.budget.compact())
                    else -> stringResource(R.string.left, subject.remaining.compact())
                },
                style = MaterialTheme.typography.bodyMedium.merge(tabular),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (running == null) FilledTonalButton({ onIntent(TodayIntent.Tap(p.id)) }) { Text(stringResource(R.string.resume, p.name), maxLines = 1, overflow = TextOverflow.Ellipsis) }
                TextButton({ onIntent(TodayIntent.Log(p.id)) }) { Text(stringResource(R.string.log_time)) }
                TextButton({ onIntent(TodayIntent.Edit(p.id)) }) { Text(stringResource(R.string.edit)) }
            }
        }
    }
}
