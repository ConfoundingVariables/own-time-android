package com.addedomarus.daybudget.alerts

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import com.addedomarus.daybudget.Commands
import com.addedomarus.daybudget.MainActivity
import com.addedomarus.daybudget.R
import com.addedomarus.daybudget.core.Alert
import com.addedomarus.daybudget.core.EventStore
import com.addedomarus.daybudget.core.Ledger
import com.addedomarus.daybudget.core.Priority
import com.addedomarus.daybudget.core.PriorityId
import com.addedomarus.daybudget.core.Row
import com.addedomarus.daybudget.core.StopReason
import com.addedomarus.daybudget.core.compact
import com.addedomarus.daybudget.core.minus
import com.addedomarus.daybudget.core.missedAlarm
import com.addedomarus.daybudget.core.plus
import com.addedomarus.daybudget.core.today
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

private const val CH_ALARM = "budget_alarm"
private const val CH_WRAP_UP = "wrap_up"
private const val CH_SESSION = "session"
private const val CH_REACHED = "budget_reached"
private const val N_ALARM = 1
private const val N_SESSION = 2
private const val N_WRAP_UP = 3
private const val N_REACHED = 4

const val EXTRA_ID = "priority_id"
const val EXTRA_AT = "at_epoch_ms"
private const val ACTION_BUDGET = "com.addedomarus.daybudget.BUDGET"
private const val ACTION_WRAP_UP = "com.addedomarus.daybudget.WRAP_UP"
private const val ACTION_TIMEOUT = "com.addedomarus.daybudget.TIMEOUT"
private const val ACTION_STOP = "com.addedomarus.daybudget.STOP"
private const val ACTION_ALARM_STOP = "com.addedomarus.daybudget.ALARM_STOP"
private const val ACTION_KEEP_GOING = "com.addedomarus.daybudget.KEEP_GOING"

private const val RING_MS = 60_000L

data class AlertAccess(val notifications: Boolean, val fullScreen: Boolean, val exactAlarms: Boolean) {
    val alarmsOn get() = notifications && fullScreen && exactAlarms
}

/** Keeps Android's alarms and notifications in step with the ledger: one exact alarm per alert in [today]. */
@Singleton
class Alerts @Inject constructor(
    @ApplicationContext private val context: Context,
    private val store: EventStore,
    private val commands: Commands,
    private val scope: CoroutineScope,
) {
    private val alarms = context.getSystemService(AlarmManager::class.java)
    private val notifications = context.getSystemService(NotificationManager::class.java)
    private val _ringing = MutableStateFlow<Instant?>(null)
    val ringing: StateFlow<Instant?> = _ringing

    fun start() {
        createChannels()
        scope.launch { store.ledger.collect(::sync) }
    }

    fun access() = AlertAccess(
        notifications = notifications.areNotificationsEnabled(),
        fullScreen = Build.VERSION.SDK_INT < 34 || notifications.canUseFullScreenIntent(),
        exactAlarms = alarms.canScheduleExactAlarms(),
    )

    /** The settings page that turns alarms back on, for the first missing access. */
    fun fixIntent(): Intent {
        val access = access()
        val pkg = Uri.parse("package:${context.packageName}")
        return when {
            !access.notifications -> Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            !access.fullScreen && Build.VERSION.SDK_INT >= 34 -> Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, pkg)
            else -> Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, pkg)
        }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    private fun sync(ledger: Ledger) {
        val now = Instant.now()
        val today = today(ledger, now, ZoneId.systemDefault())
        schedule(ACTION_BUDGET, today.alerts.firstOrNull { it is Alert.BudgetAlarm }, now)
        schedule(ACTION_WRAP_UP, today.alerts.firstOrNull { it is Alert.WrapUpNotice }, now)
        val running = today.running
        if (running == null) {
            notifications.cancel(N_SESSION)
            notifications.cancel(N_WRAP_UP)
        } else {
            notifications.notify(N_SESSION, sessionNotification(running, now))
        }
    }

    suspend fun handle(intent: Intent) {
        val id = intent.getStringExtra(EXTRA_ID)?.let(::PriorityId)
        val at = Instant.ofEpochMilli(intent.getLongExtra(EXTRA_AT, 0))
        when (intent.action) {
            ACTION_BUDGET -> onBudgetAlarm(id!!, at)
            ACTION_WRAP_UP -> onWrapUp(id!!)
            ACTION_TIMEOUT -> onTimeout(id!!, at)
            ACTION_STOP -> commands.stop(id!!, StopReason.User)
            ACTION_ALARM_STOP -> stop(id!!)
            ACTION_KEEP_GOING -> keepGoing(id!!)
            Intent.ACTION_BOOT_COMPLETED -> onBoot()
            else -> sync(store.ledgerNow())
        }
    }

    suspend fun stop(id: PriorityId) {
        silence()
        commands.stop(id, StopReason.AlarmStop)
    }

    suspend fun keepGoing(id: PriorityId) {
        silence()
        commands.keepGoing(id)
    }

    private suspend fun onBudgetAlarm(id: PriorityId, at: Instant) {
        val ledger = store.ledgerNow()
        val row = today(ledger, Instant.now(), ZoneId.systemDefault()).running
        if (row?.priority?.id != id || row.remaining > 1.seconds) return sync(ledger)
        _ringing.value = at
        notifications.notify(N_ALARM, alarmNotification(row.priority, at))
        alarms.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + RING_MS, pending(ACTION_TIMEOUT, id, at))
    }

    private suspend fun onWrapUp(id: PriorityId) {
        val row = today(store.ledgerNow(), Instant.now(), ZoneId.systemDefault()).running
        if (row?.priority?.id != id || row.remaining <= ZERO) return
        notifications.notify(N_WRAP_UP, notification(CH_WRAP_UP, row.priority)
            .setContentTitle(context.getString(R.string.wrap_up_title, row.priority.name))
            .setContentText(context.getString(R.string.left, row.remaining.compact()))
            .setPublicVersion(notification(CH_WRAP_UP, null).setContentTitle(context.getString(R.string.wrap_up_public)).build())
            .setVisibility(row.priority.visibility(Notification.VISIBILITY_PRIVATE))
            .setAutoCancel(true)
            .build())
    }

    private suspend fun onTimeout(id: PriorityId, ranOut: Instant) {
        silence()
        if (commands.endAt(id, ranOut)) postReached(store.ledgerNow().priorities[id], ranOut)
    }

    private suspend fun onBoot() {
        val ledger = store.ledgerNow()
        val id = ledger.running?.id
        val ranOut = missedAlarm(ledger, Instant.now(), ZoneId.systemDefault())
        if (id != null && ranOut != null && commands.endAt(id, ranOut)) postReached(ledger.priorities[id], ranOut)
        sync(store.ledgerNow())
    }

    private fun silence() {
        notifications.cancel(N_ALARM)
        cancel(ACTION_TIMEOUT)
        _ringing.value = null
    }

    private fun postReached(priority: Priority?, ranOut: Instant) {
        priority ?: return
        val time = ranOut.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
        notifications.notify(N_REACHED, notification(CH_REACHED, priority)
            .setContentTitle(context.getString(R.string.budget_reached))
            .setContentText(context.getString(R.string.budget_reached_text, priority.name, time))
            .setPublicVersion(notification(CH_REACHED, null).setContentTitle(context.getString(R.string.budget_reached)).build())
            .setVisibility(priority.visibility(Notification.VISIBILITY_PRIVATE))
            .setAutoCancel(true)
            .build())
    }

    private fun alarmNotification(priority: Priority, at: Instant): Notification {
        val fullScreen = PendingIntent.getActivity(
            context, 0,
            Intent(context, AlarmActivity::class.java)
                .putExtra(EXTRA_ID, priority.id.value).putExtra(EXTRA_AT, at.toEpochMilli())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val public = notification(CH_ALARM, null)
            .setContentTitle(context.getString(R.string.times_up))
            .setContentText(context.getString(R.string.budget_reached))
            .build()
        return notification(CH_ALARM, priority)
            .setContentTitle(context.getString(R.string.times_up))
            .setContentText(context.getString(R.string.priority_budget_reached, priority.name))
            .setCategory(Notification.CATEGORY_ALARM)
            .setVisibility(priority.visibility(Notification.VISIBILITY_PRIVATE))
            .setPublicVersion(public)
            .setFullScreenIntent(fullScreen, true)
            .setContentIntent(fullScreen)
            .setOngoing(true)
            .addAction(Notification.Action.Builder(null, context.getString(R.string.stop), pending(ACTION_ALARM_STOP, priority.id)).build())
            .addAction(Notification.Action.Builder(null, context.getString(R.string.keep_going), pending(ACTION_KEEP_GOING, priority.id)).build())
            .build()
            .apply { flags = flags or Notification.FLAG_INSISTENT }
    }

    private fun sessionNotification(row: Row, now: Instant): Notification {
        val overtime = row.overtime > ZERO
        return notification(CH_SESSION, row.priority)
            .setContentTitle(row.priority.name)
            .setContentText(
                if (overtime) context.getString(R.string.over, row.overtime.compact())
                else "· " + context.getString(R.string.of_budget_today, row.budget.compact()),
            )
            .setUsesChronometer(true)
            .setChronometerCountDown(!overtime)
            .setWhen((if (overtime) now - row.overtime else now + row.remaining).toEpochMilli())
            .setShowWhen(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setVisibility(row.priority.visibility(Notification.VISIBILITY_SECRET))
            .addAction(Notification.Action.Builder(null, context.getString(R.string.stop), pending(ACTION_STOP, row.priority.id)).build())
            .build()
    }

    private fun Priority.visibility(hidden: Int) = if (hideOnLockScreen) hidden else Notification.VISIBILITY_PUBLIC

    private fun notification(channel: String, priority: Priority?) = Notification.Builder(context, channel)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentIntent(
            PendingIntent.getActivity(
                context, 1,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_IMMUTABLE,
            ),
        )
        .apply { priority?.let { setGroup(it.id.value) } }

    private fun schedule(action: String, alert: Alert?, now: Instant) {
        if (alert == null) return cancel(action)
        val inMs = (alert.at.toEpochMilli() - now.toEpochMilli()).coerceAtLeast(0)
        alarms.setExactAndAllowWhileIdle(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + inMs,
            pending(action, alert.id, alert.at),
        )
    }

    private fun cancel(action: String) {
        PendingIntent.getBroadcast(context, action.hashCode(), Intent(context, AlertReceiver::class.java).setAction(action),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)?.let(alarms::cancel)
    }

    private fun pending(action: String, id: PriorityId, at: Instant? = null): PendingIntent = PendingIntent.getBroadcast(
        context, action.hashCode(),
        Intent(context, AlertReceiver::class.java).setAction(action)
            .putExtra(EXTRA_ID, id.value).putExtra(EXTRA_AT, at?.toEpochMilli() ?: 0),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun createChannels() {
        val alarmSound = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        notifications.createNotificationChannels(listOf(
            NotificationChannel(CH_ALARM, context.getString(R.string.channel_alarm), NotificationManager.IMPORTANCE_HIGH).apply {
                setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), alarmSound)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 600, 400, 600, 400)
                lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            },
            NotificationChannel(CH_WRAP_UP, context.getString(R.string.channel_wrap_up), NotificationManager.IMPORTANCE_HIGH),
            NotificationChannel(CH_SESSION, context.getString(R.string.channel_session), NotificationManager.IMPORTANCE_LOW),
            NotificationChannel(CH_REACHED, context.getString(R.string.channel_reached), NotificationManager.IMPORTANCE_LOW),
        ))
    }
}

/** Alarm, notification-action, boot, clock, and update broadcasts all funnel into [Alerts.handle]. */
@AndroidEntryPoint
class AlertReceiver : BroadcastReceiver() {
    @Inject lateinit var alerts: Alerts
    @Inject lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        scope.launch {
            try { alerts.handle(intent) } finally { pending.finish() }
        }
    }
}
