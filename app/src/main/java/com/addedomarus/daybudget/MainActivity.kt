package com.addedomarus.daybudget

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.addedomarus.daybudget.ui.ActionsSheet
import com.addedomarus.daybudget.ui.DayBudgetTheme
import com.addedomarus.daybudget.ui.DeleteDialog
import com.addedomarus.daybudget.ui.EditorSheet
import com.addedomarus.daybudget.ui.LogSheet
import com.addedomarus.daybudget.ui.ManageScreen
import com.addedomarus.daybudget.ui.OnboardingScreen
import com.addedomarus.daybudget.ui.TodayIntent
import com.addedomarus.daybudget.ui.TodayScreen
import com.addedomarus.daybudget.ui.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable private data object OnboardingRoute
@Serializable private data object TodayRoute
@Serializable private data object ManageRoute

private sealed interface Sheet {
    data class Actions(val id: com.addedomarus.daybudget.core.PriorityId) : Sheet
    data class Editor(val id: com.addedomarus.daybudget.core.PriorityId?) : Sheet
    data class Log(val id: com.addedomarus.daybudget.core.PriorityId) : Sheet
    data class Delete(val id: com.addedomarus.daybudget.core.PriorityId) : Sheet
    data object About : Sheet
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val onboardingDone by lazy { getSharedPreferences("day-budget", MODE_PRIVATE).getBoolean("onboarded", false) }
    private val requestAlerts = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val vm: MainViewModel by viewModels()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) { vm.refreshAccess() }
        }
        setContent {
            DayBudgetTheme {
                var sheet by remember { mutableStateOf<Sheet?>(null) }
                val nav = rememberNavController()
                fun onIntent(intent: TodayIntent) = when (intent) {
                    is TodayIntent.Tap -> vm.tap(intent.id)
                    is TodayIntent.Hold -> sheet = Sheet.Actions(intent.id)
                    is TodayIntent.Log -> sheet = Sheet.Log(intent.id)
                    is TodayIntent.Edit -> sheet = Sheet.Editor(intent.id)
                    TodayIntent.Add -> sheet = Sheet.Editor(null)
                    TodayIntent.Manage -> nav.navigate(ManageRoute)
                    TodayIntent.Export -> lifecycleScope.launch { startActivity(vm.export().let(vm::share)) }
                    TodayIntent.About -> sheet = Sheet.About
                    TodayIntent.FixAlarms -> startActivity(vm.fixIntent())
                    TodayIntent.DismissBanner -> vm.dismissBanner()
                }
                NavHost(nav, startDestination = if (onboardingDone) TodayRoute else OnboardingRoute) {
                    composable<OnboardingRoute> {
                        OnboardingScreen(
                            onAllowAlerts = { if (Build.VERSION.SDK_INT >= 33) requestAlerts.launch(Manifest.permission.POST_NOTIFICATIONS) },
                            onDone = {
                                getSharedPreferences("day-budget", MODE_PRIVATE).edit().putBoolean("onboarded", true).apply()
                                nav.navigate(TodayRoute) { popUpTo(OnboardingRoute) { inclusive = true } }
                            },
                        )
                    }
                    composable<TodayRoute> {
                        val ui by vm.state.collectAsStateWithLifecycle()
                        ui?.let { TodayScreen(it, ::onIntent) }
                    }
                    composable<ManageRoute> {
                        val ui by vm.state.collectAsStateWithLifecycle()
                        ui?.let { state ->
                            ManageScreen(
                                state.ledger,
                                onEdit = { sheet = Sheet.Editor(it) },
                                onAdd = { sheet = Sheet.Editor(null) },
                                onBack = { nav.popBackStack() },
                            )
                        }
                    }
                }
                SheetsHost(sheet, vm) { sheet = it }
            }
        }
    }
}

@Composable
private fun SheetsHost(sheet: Sheet?, vm: MainViewModel, set: (Sheet?) -> Unit) {
    val context = LocalContext.current
    if (sheet == null) return
    when (sheet) {
        is Sheet.Actions -> {
            val name = vm.state.value?.ledger?.priorities?.get(sheet.id)?.name ?: return
            ActionsSheet(
                name,
                onLog = { set(Sheet.Log(sheet.id)) },
                onEdit = { set(Sheet.Editor(sheet.id)) },
                onDelete = { set(Sheet.Delete(sheet.id)) },
                onDismiss = { set(null) },
            )
        }
        is Sheet.Editor -> {
            val ledger = vm.state.value?.ledger
            val existing = sheet.id?.let { ledger?.priorities?.get(it) }
            val schedule = sheet.id?.let { ledger?.schedule(it) } ?: com.addedomarus.daybudget.core.WeeklySchedule.every(60)
            EditorSheet(
                existing,
                schedule,
                alarmsOn = vm.alarmsOn,
                onFixAlarms = { context.startActivity(vm.fixIntent()) },
                onSave = { p, s -> vm.save(p, s); set(null) },
                onDelete = { set(Sheet.Delete(sheet.id!!)) },
                onDismiss = { set(null) },
            )
        }
        is Sheet.Log -> {
            val ui = vm.state.value ?: return
            LogSheet(
                ui.ledger.priorities[sheet.id]?.name ?: return,
                ui.today.day,
                onApply = { day, amount -> vm.adjust(sheet.id, day, amount); set(null) },
                onDismiss = { set(null) },
            )
        }
        is Sheet.Delete -> {
            val name = vm.state.value?.ledger?.priorities?.get(sheet.id)?.name ?: return
            DeleteDialog(
                name,
                onKeepHistory = { vm.delete(sheet.id); set(null) },
                onErase = { vm.erase(sheet.id); set(null) },
                onDismiss = { set(null) },
            )
        }
        Sheet.About -> AboutSheet { set(null) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "?"
    }
    ModalBottomSheet(onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.about), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.about_version, version), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.about_privacy), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.about_export), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.about_licences), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onDismiss) { Text(stringResource(R.string.close)) }
        }
    }
}
