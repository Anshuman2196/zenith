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
import com.zenith.launcher.R

/** Schedules a completion reminder that survives Zenith being backgrounded. */
object TimerAlarmScheduler {
    private const val ACTION_TIMER_COMPLETE = "com.zenith.launcher.TIMER_COMPLETE"
    private const val EXTRA_MESSAGE = "message"
    internal const val REQUEST_CODE = 2401
    private const val ALARM_REQUEST_CODE = 2402

    fun schedule(context: Context, seconds: Int, message: String) {
        val alarm = context.getSystemService(AlarmManager::class.java) ?: return
        alarm.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + seconds * 1000L, pendingIntent(context, message))
    }
    fun cancel(context: Context) {
        val alarm = context.getSystemService(AlarmManager::class.java) ?: return
        alarm.cancel(pendingIntent(context, ""))
    }
    fun scheduleAlarm(context: Context, hour: Int, minute: Int, message: String) {
        val alarm = context.getSystemService(AlarmManager::class.java) ?: return
        val trigger = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, hour)
            set(java.util.Calendar.MINUTE, minute)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(java.util.Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis
        alarm.setAlarmClock(AlarmManager.AlarmClockInfo(trigger, alarmPendingIntent(context, message)), alarmPendingIntent(context, message))
    }
    fun cancelAlarm(context: Context) {
        val alarm = context.getSystemService(AlarmManager::class.java) ?: return
        alarm.cancel(alarmPendingIntent(context, ""))
    }
    private fun pendingIntent(context: Context, message: String): PendingIntent = PendingIntent.getBroadcast(
        context, REQUEST_CODE, Intent(context, TimerAlarmReceiver::class.java).setAction(ACTION_TIMER_COMPLETE).putExtra(EXTRA_MESSAGE, message),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    private fun alarmPendingIntent(context: Context, message: String): PendingIntent = PendingIntent.getBroadcast(
        context, ALARM_REQUEST_CODE, Intent(context, TimerAlarmReceiver::class.java).setAction(ACTION_TIMER_COMPLETE).putExtra(EXTRA_MESSAGE, message),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    internal fun message(intent: Intent) = intent.getStringExtra(EXTRA_MESSAGE) ?: "Timer complete"
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
        manager.notify(TimerAlarmScheduler.REQUEST_CODE, notification)
    }
}
