package com.addedomarus.daybudget.alerts

import android.app.KeyguardManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.addedomarus.daybudget.R
import com.addedomarus.daybudget.core.EventStore
import com.addedomarus.daybudget.core.PriorityId
import com.addedomarus.daybudget.ui.DayBudgetTheme
import com.addedomarus.daybudget.ui.color
import dagger.hilt.android.AndroidEntryPoint
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AlarmActivity : ComponentActivity() {
    @Inject lateinit var alerts: Alerts
    @Inject lateinit var store: EventStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        val id = PriorityId(intent.getStringExtra(EXTRA_ID)!!)
        val at = Instant.ofEpochMilli(intent.getLongExtra(EXTRA_AT, 0))
        val locked = getSystemService(KeyguardManager::class.java).isKeyguardLocked
        lifecycleScope.launch {
            if (alerts.ringing.value == null) finish() else alerts.ringing.drop(1).first { it == null }.also { finish() }
        }
        setContent {
            DayBudgetTheme {
                val ledger by store.ledger.collectAsState(null)
                val priority = ledger?.priorities?.get(id)
                AlarmScreen(
                    name = priority?.name?.takeUnless { priority.hideOnLockScreen && locked },
                    accent = priority?.color?.color() ?: MaterialTheme.colorScheme.primary,
                    time = at.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)),
                    onStop = { lifecycleScope.launch { alerts.stop(id); finish() } },
                    onKeepGoing = { lifecycleScope.launch { alerts.keepGoing(id); finish() } },
                )
            }
        }
    }
}

@Composable
fun AlarmScreen(name: String?, accent: androidx.compose.ui.graphics.Color, time: String, onStop: () -> Unit, onKeepGoing: () -> Unit) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(
            Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(time, style = MaterialTheme.typography.displayLarge)
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.times_up), style = MaterialTheme.typography.headlineMedium)
            Text(
                if (name == null) stringResource(R.string.budget_reached) else stringResource(R.string.priority_budget_reached, name),
                style = MaterialTheme.typography.titleLarge,
                color = accent,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(48.dp))
            Button(onStop, Modifier.fillMaxWidth().height(64.dp)) { Text(stringResource(R.string.stop), style = MaterialTheme.typography.titleMedium) }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onKeepGoing, Modifier.fillMaxWidth().height(56.dp)) { Text(stringResource(R.string.keep_going)) }
        }
    }
}
