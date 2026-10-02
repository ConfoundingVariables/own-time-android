package com.addedomarus.daybudget.ui

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.addedomarus.daybudget.R
import com.addedomarus.daybudget.core.EventDao
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class TodayUi(val day: LocalDate)

@HiltViewModel
class TodayViewModel @Inject constructor(events: EventDao) : ViewModel() {
    val state = events.observe()
        .map { TodayUi(LocalDate.now()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@Composable
fun TodayRoute(viewModel: TodayViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    state?.let { TodayScreen(it, onAdd = {}) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(state: TodayUi, onAdd: () -> Unit) {
    val locale = Locale.getDefault()
    val date = state.day.format(DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "EEEEdMMMM"), locale))
    Scaffold(
        topBar = {
            TopAppBar(title = {
                Column {
                    Text(stringResource(R.string.today))
                    Text(date, style = MaterialTheme.typography.bodyMedium)
                }
            })
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.nothing_planned), style = MaterialTheme.typography.titleLarge)
            Button(onClick = onAdd) { Text(stringResource(R.string.add_first_priority)) }
        }
    }
}
