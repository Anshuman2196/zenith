package com.zenith.launcher.data.model

import kotlinx.serialization.Serializable

/** ON/OFF switch for every toggleable home-screen widget - maps 1:1 to a Settings toggle row. */
@Serializable
data class WidgetVisibility(
    val countdownEnabled: Boolean = false,
    val pomodoroEnabled: Boolean = false,
    val todoEnabled: Boolean = false,
    val chapterBacklogEnabled: Boolean = false,
    val pdfLauncherEnabled: Boolean = false,
    val focusModeEnabled: Boolean = false,
    val milestoneEnabled: Boolean = false,
    val appShortcutsEnabled: Boolean = false,
    val systemStatusEnabled: Boolean = false
)
