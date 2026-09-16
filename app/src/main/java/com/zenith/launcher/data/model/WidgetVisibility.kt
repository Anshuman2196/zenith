package com.zenith.launcher.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/** ON/OFF switch for every toggleable home-screen widget - maps 1:1 to a Settings toggle row. */
@Serializable
data class WidgetVisibility(
    @SerialName("countdownEnabled") val deadlinesEnabled: Boolean = false,
    val pomodoroEnabled: Boolean = false,
    val todoEnabled: Boolean = false,
    @SerialName("chapterBacklogEnabled") val backlogEnabled: Boolean = false,
    @SerialName("pdfLauncherEnabled") val libraryEnabled: Boolean = false,
    val focusModeEnabled: Boolean = false,
    @SerialName("milestoneEnabled") val targetsEnabled: Boolean = false,
    val appShortcutsEnabled: Boolean = false,
    val systemStatusEnabled: Boolean = false
)
