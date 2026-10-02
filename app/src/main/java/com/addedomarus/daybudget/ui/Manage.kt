package com.addedomarus.daybudget.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.addedomarus.daybudget.R
import com.addedomarus.daybudget.core.Ledger
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageScreen(ledger: Ledger, onEdit: (com.addedomarus.daybudget.core.PriorityId) -> Unit, onAdd: () -> Unit, onBack: () -> Unit) {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.manage_priorities)) },
                navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(text = { Text(stringResource(R.string.add)) }, icon = { Icon(Icons.Default.Add, null) }, onClick = onAdd)
        },
    ) { padding ->
        val priorities = ledger.live.sortedBy { it.name }
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            items(priorities, key = { it.id.value }) { p ->
                val notToday = ledger.budget(p.id, today) == kotlin.time.Duration.ZERO
                ListItem(
                    headlineContent = { Text(p.name) },
                    supportingContent = { Text(ledger.schedule(p.id).summary(), style = MaterialTheme.typography.bodySmall) },
                    leadingContent = { Box(Modifier.size(14.dp).background(p.color.color(), CircleShape)) },
                    trailingContent = { if (notToday) NotTodayBadge() },
                    modifier = Modifier.clickable { onEdit(p.id) },
                )
            }
            item { Box(Modifier.size(96.dp)) }
        }
    }
}

@Composable
fun NotTodayBadge() {
    Text(
        stringResource(R.string.not_today),
        Modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
