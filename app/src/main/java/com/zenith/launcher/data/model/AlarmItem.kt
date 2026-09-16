package com.zenith.launcher.data.model

import kotlinx.serialization.Serializable

/**
 * One user-scheduled daily alarm, managed from the Study Timer widget's Alarm tab. Several can
 * exist at once (see [com.zenith.launcher.data.local.PreferencesManager.alarms]); each is
 * independently enabled/disabled and scheduled with its own [id] so they never overwrite one
 * another's system [android.app.AlarmManager] entry.
 */
@Serializable
data class AlarmItem(
    val id: String,
    val hour: Int,
    val minute: Int,
    val label: String = "Zenith alarm",
    val isEnabled: Boolean = true
)
