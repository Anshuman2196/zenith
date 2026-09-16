package com.zenith.launcher.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.provider.Settings
import com.zenith.launcher.MainActivity
import com.zenith.launcher.R

/** Schedules a completion reminder that survives Zenith being backgrounded. */
object TimerAlarmScheduler {
    private const val ACTION_TIMER_COMPLETE = "com.zenith.launcher.TIMER_COMPLETE"
    private const val EXTRA_MESSAGE = "message"
    private const val EXTRA_NOTIFICATION_ID = "notification_id"
    internal const val REQUEST_CODE = 2401
    private const val SHOW_INTENT_REQUEST_CODE = 2402

    // User-created alarms (see AlarmSection in PomodoroWidget.kt) each get their own PendingIntent
    // request code, derived from the alarm's id, so multiple alarms can be scheduled at once
    // without one silently overwriting another's PendingIntent.
    private const val ALARM_REQUEST_CODE_BASE = 100_000
    private const val ALARM_REQUEST_CODE_RANGE = 100_000

    fun schedule(context: Context, seconds: Int, message: String) {
        val alarm = context.getSystemService(AlarmManager::class.java) ?: return
        alarm.setAndAllowWhileIdle(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + seconds * 1000L,
            pendingIntent(context, REQUEST_CODE, message)
        )
    }

    fun cancel(context: Context) {
        val alarm = context.getSystemService(AlarmManager::class.java) ?: return
        alarm.cancel(pendingIntent(context, REQUEST_CODE, ""))
    }

    /** Schedules (or reschedules) one user alarm. [alarmId] must be stable across calls for the
     * same alarm - it's what lets this alarm be individually rescheduled or cancelled later
     * without disturbing any other alarm. */
    fun scheduleAlarm(context: Context, alarmId: String, hour: Int, minute: Int, message: String) {
        val alarm = context.getSystemService(AlarmManager::class.java) ?: return
        val trigger = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, hour)
            set(java.util.Calendar.MINUTE, minute)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(java.util.Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis
        val requestCode = alarmRequestCode(alarmId)
        // The AlarmClockInfo "show" intent is what the system launches if the user taps the
        // status-bar/quick-settings "next alarm" affordance - it must be an *activity* PendingIntent.
        // Previously this passed the same broadcast PendingIntent used to actually fire the alarm,
        // which Android can't resolve to a UI and falls back to an app-chooser sheet instead.
        alarm.setAlarmClock(
            AlarmManager.AlarmClockInfo(trigger, showIntent(context)),
            pendingIntent(context, requestCode, message)
        )
    }

    fun cancelAlarm(context: Context, alarmId: String) {
        val alarm = context.getSystemService(AlarmManager::class.java) ?: return
        alarm.cancel(pendingIntent(context, alarmRequestCode(alarmId), ""))
    }

    /** Stable positive request code derived from an alarm's id, so each alarm gets its own PendingIntent. */
    private fun alarmRequestCode(alarmId: String): Int =
        ALARM_REQUEST_CODE_BASE + (alarmId.hashCode() and Int.MAX_VALUE) % ALARM_REQUEST_CODE_RANGE

    private fun showIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context, SHOW_INTENT_REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun pendingIntent(context: Context, requestCode: Int, message: String): PendingIntent = PendingIntent.getBroadcast(
        context,
        requestCode,
        Intent(context, TimerAlarmReceiver::class.java)
            .setAction(ACTION_TIMER_COMPLETE)
            .putExtra(EXTRA_MESSAGE, message)
            .putExtra(EXTRA_NOTIFICATION_ID, requestCode),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    internal fun message(intent: Intent) = intent.getStringExtra(EXTRA_MESSAGE) ?: "Timer complete"

    /** Per-alarm notification id (defaults to the Pomodoro timer's own id for older intents). */
    internal fun notificationId(intent: Intent) = intent.getIntExtra(EXTRA_NOTIFICATION_ID, REQUEST_CODE)
}

class TimerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel("study_timer", "Study timer", NotificationManager.IMPORTANCE_HIGH)
        manager.createNotificationChannel(channel)
        val notification = android.app.Notification.Builder(context, "study_timer")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Zenith study timer")
            .setContentText(TimerAlarmScheduler.message(intent))
            .setSound(Settings.System.DEFAULT_ALARM_ALERT_URI)
            .setCategory(android.app.Notification.CATEGORY_ALARM)
            .setAutoCancel(true)
            .build()
        manager.notify(TimerAlarmScheduler.notificationId(intent), notification)
    }
}
