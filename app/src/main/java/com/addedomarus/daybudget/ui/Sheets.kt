package com.addedomarus.daybudget.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.addedomarus.daybudget.R
import com.addedomarus.daybudget.core.PaletteColor
import com.addedomarus.daybudget.core.Priority
import com.addedomarus.daybudget.core.WeeklySchedule
import com.addedomarus.daybudget.core.precise
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

private val PRESETS = listOf(30, 60, 120, 240)
private val DAYS = DayOfWeek.entries.map { it.name.take(3) }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorSheet(
    existing: Priority?,
    initialSchedule: WeeklySchedule,
    alarmsOn: Boolean,
    onFixAlarms: () -> Unit,
    onSave: (Priority, WeeklySchedule) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val p = existing ?: Priority(
        com.addedomarus.daybudget.core.PriorityId.random(),
        "",
        PaletteColor.entries.first(),
    )
    var name by remember { mutableStateOf(p.name) }
    var colour by remember { mutableStateOf(p.color) }
    var wrapUp by remember { mutableIntStateOf(p.wrapUpMinutes) }
    var hidden by remember { mutableStateOf(p.hideOnLockScreen) }
    var perWeekday by remember { mutableStateOf(!initialSchedule.sameEveryDay) }
    var same by remember { mutableIntStateOf(if (initialSchedule.sameEveryDay) initialSchedule.minutes.first() else 60) }
    var days by remember { mutableStateOf(initialSchedule.minutes.toIntArray()) }

    val schedule = if (perWeekday) WeeklySchedule(days.toList()) else WeeklySchedule.every(same)

    ModalBottomSheet(onDismiss) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                for (c in PaletteColor.entries) {
                    val selected = c == colour
                    Box(
                        Modifier
                            .size(40.dp)
                            .background(c.color(), CircleShape)
                            .then(
                                if (selected) Modifier.border(androidx.compose.foundation.BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface), CircleShape)
                                else Modifier,
                            )
                            .clickable { colour = c }
                            .semantics { contentDescription = c.name },
                    )
                }
            }

            androidx.compose.material3.OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(selected = !perWeekday, onClick = { perWeekday = false }, shape = SegmentedButtonDefaults.itemShape(0, 2)) {
                    Text(stringResource(R.string.same_every_day))
                }
                SegmentedButton(selected = perWeekday, onClick = { perWeekday = true }, shape = SegmentedButtonDefaults.itemShape(1, 2)) {
                    Text(stringResource(R.string.per_weekday))
                }
            }

            if (!perWeekday) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.daily_budget), style = MaterialTheme.typography.titleMedium)
                    Text(same.minutes.precise(), style = MaterialTheme.typography.headlineMedium.merge(tabular))
                    Slider(same.toFloat(), { same = it.toInt() }, valueRange = 15f..480f, steps = 30)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (preset in PRESETS) AssistChip({ same = preset }, { Text(preset.minutes.precise()) })
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(stringResource(R.string.daily_budget), style = MaterialTheme.typography.titleMedium)
                    for (i in 0..6) Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(DAYS[i], Modifier.size(44.dp), style = MaterialTheme.typography.bodyLarge)
                        Slider(
                            days[i].toFloat(),
                            { days = days.copyOf().also { d -> d[i] = it.toInt() } },
                            valueRange = 0f..480f,
                            steps = 31,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            if (days[i] == 0) stringResource(R.string.off) else days[i].minutes.precise(),
                            Modifier.size(64.dp),
                            style = MaterialTheme.typography.bodyMedium.merge(tabular),
                            color = if (days[i] == 0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.wrap_up_notification), style = MaterialTheme.typography.titleMedium)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    val options = listOf(0, 5, 10, 15)
                    options.forEachIndexed { i, minutes ->
                        SegmentedButton(
                            selected = wrapUp == minutes,
                            onClick = { wrapUp = minutes },
                            shape = SegmentedButtonDefaults.itemShape(i, options.size),
                        ) { Text(if (minutes == 0) stringResource(R.string.off) else "${minutes}m", maxLines = 1) }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.hide_on_lock_screen), style = MaterialTheme.typography.bodyLarge)
                    Text(stringResource(R.string.hide_on_lock_screen_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(hidden, { hidden = it })
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Notifications, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    if (alarmsOn) stringResource(R.string.alarms_on_hint) else stringResource(R.string.alarms_off),
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (alarmsOn) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                )
                if (!alarmsOn) TextButton(onFixAlarms) { Text(stringResource(R.string.fix)) }
            }

            Row(Modifier.padding(bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (existing != null) {
                    TextButton(onDelete) {
                        Icon(Icons.Default.Delete, null)
                        Spacer4()
                        Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                    }
                }
                Box(Modifier.weight(1f))
                OutlinedButton(onDismiss) { Text(stringResource(R.string.cancel)) }
                Button(
                    { onSave(p.copy(id = existing?.id ?: p.id, name = name.trim(), color = colour, wrapUpMinutes = wrapUp, hideOnLockScreen = hidden), schedule) },
                    enabled = name.isNotBlank(),
                ) { Text(stringResource(R.string.save)) }
            }
        }
    }
}

@Composable private fun Spacer4() = Box(Modifier.size(4.dp))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogSheet(name: String, today: LocalDate, onApply: (LocalDate, kotlin.time.Duration) -> Unit, onDismiss: () -> Unit) {
    var yesterday by remember { mutableStateOf(false) }
    var minutes by remember { mutableIntStateOf(0) }

    ModalBottomSheet(onDismiss) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.log_time_for, name), style = MaterialTheme.typography.titleLarge)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(!yesterday, { yesterday = false }, shape = SegmentedButtonDefaults.itemShape(0, 2)) { Text(stringResource(R.string.today_tab)) }
                SegmentedButton(yesterday, { yesterday = true }, shape = SegmentedButtonDefaults.itemShape(1, 2)) { Text(stringResource(R.string.yesterday)) }
            }
            Text(
                if (minutes == 0) stringResource(R.string.nothing_selected)
                else if (minutes > 0) stringResource(R.string.add_time, minutes.minutes.precise())
                else stringResource(R.string.remove_time, (-minutes).minutes.precise()),
                style = MaterialTheme.typography.headlineSmall.merge(tabular),
                color = if (minutes < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
            Slider(minutes.toFloat(), { minutes = it.toInt() }, valueRange = -240f..240f, steps = 95)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (preset in listOf(-60, -30, -15, 15, 30, 60)) {
                    AssistChip({ minutes = preset }, { Text(if (preset > 0) "+${preset.minutes.precise()}" else preset.minutes.precise()) })
                }
            }
            Text(stringResource(R.string.log_help), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f))
                OutlinedButton(onDismiss) { Text(stringResource(R.string.cancel)) }
                Button({ onApply(if (yesterday) today.minusDays(1) else today, minutes.minutes) }, enabled = minutes != 0) {
                    Text(stringResource(R.string.apply))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionsSheet(name: String, onLog: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismiss) {
        Column(Modifier.padding(bottom = 32.dp)) {
            ActionRow(Icons.Default.DateRange, stringResource(R.string.log_time), onLog)
            ActionRow(Icons.Default.Edit, stringResource(R.string.edit), onEdit)
            ActionRow(Icons.Default.Delete, stringResource(R.string.delete), onDelete, destructive = true)
        }
    }
}

@Composable
private fun ActionRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit, destructive: Boolean = false) {
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 24.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(icon, null, tint = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
        Text(label, style = MaterialTheme.typography.bodyLarge, color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun DeleteDialog(name: String, onKeepHistory: () -> Unit, onErase: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_priority, name)) },
        text = { Text(stringResource(R.string.delete_warning)) },
        confirmButton = { TextButton(onKeepHistory) { Text(stringResource(R.string.delete_keep_history)) } },
        dismissButton = {
            Row {
                TextButton(onDismiss) { Text(stringResource(R.string.cancel)) }
                TextButton(onErase) { Text(stringResource(R.string.erase_everything), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
            }
        },
    )
}

@Composable
fun WeeklySchedule.summary(): String {
    val off = stringResource(R.string.off)
    fun day(i: Int) = minutes[i].let { if (it == 0) off else it.minutes.precise() }
    return if (sameEveryDay) stringResource(R.string.every_day, day(0))
    else DayOfWeek.entries.joinToString(" · ") { d -> "${d.name.take(3)} ${day(d.ordinal)}" }
}
