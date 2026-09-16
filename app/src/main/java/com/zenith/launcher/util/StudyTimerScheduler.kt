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

/** Schedules a study-phase completion reminder that survives Zenith being backgrounded. */
object StudyTimerScheduler {
    private const val ACTION_TIMER_COMPLETE = "com.zenith.launcher.TIMER_COMPLETE"
    private const val EXTRA_MESSAGE = "message"
    private const val EXTRA_NOTIFICATION_ID = "notification_id"
    internal const val REQUEST_CODE = 2401

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

    // Android AlarmManager is used only as the OS-backed delivery mechanism for Pomodoro completion.
    private fun pendingIntent(context: Context, requestCode: Int, message: String): PendingIntent = PendingIntent.getBroadcast(
        context,
        requestCode,
        Intent(context, StudyTimerReceiver::class.java)
            .setAction(ACTION_TIMER_COMPLETE)
            .putExtra(EXTRA_MESSAGE, message)
            .putExtra(EXTRA_NOTIFICATION_ID, requestCode),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    internal fun message(intent: Intent) = intent.getStringExtra(EXTRA_MESSAGE) ?: "Study timer complete"

    /** Notification id (defaults to the Pomodoro timer's own id for older intents). */
    internal fun notificationId(intent: Intent) = intent.getIntExtra(EXTRA_NOTIFICATION_ID, REQUEST_CODE)
}

class StudyTimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel("study_timer", "Study timer", NotificationManager.IMPORTANCE_HIGH)
        manager.createNotificationChannel(channel)
        val notification = android.app.Notification.Builder(context, "study_timer")
            .setSmallIcon(R.mipmap.ic_zenith)
            .setContentTitle("Zenith study timer")
            .setContentText(StudyTimerScheduler.message(intent))
            .setSound(Settings.System.DEFAULT_ALARM_ALERT_URI)
            .setCategory(android.app.Notification.CATEGORY_ALARM)
            .setAutoCancel(true)
            .build()
        manager.notify(StudyTimerScheduler.notificationId(intent), notification)
    }
}
