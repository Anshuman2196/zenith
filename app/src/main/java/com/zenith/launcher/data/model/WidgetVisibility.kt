package com.zenith.launcher.data.model

import kotlinx.serialization.Serializable

/** ON/OFF switch for every toggleable home-screen widget - maps 1:1 to a Settings toggle row. */
@Serializable
data class WidgetVisibility(
    val countdownEnabled: Boolean = true,
    val pomodoroEnabled: Boolean = true,
    val todoEnabled: Boolean = true,
    val chapterBacklogEnabled: Boolean = true,
    val pdfLauncherEnabled: Boolean = true,
    val focusModeEnabled: Boolean = true,
    val milestoneEnabled: Boolean = true,
    val appShortcutsEnabled: Boolean = true,
    val systemStatusEnabled: Boolean = true
)
