package com.zenith.launcher.data.model

/**
 * Stable string ids for every draggable home-screen widget. Persisted (as an ordered list) in
 * DataStore via [com.zenith.launcher.data.local.PreferencesManager.widgetOrder] so a user's
 * drag-to-reorder arrangement survives app restarts.
 */
object WidgetIds {
    const val CLOCK = "clock"
    const val COUNTDOWN = "countdown"
    const val FOCUS_MODE = "focus_mode"
    const val POMODORO = "pomodoro"
    const val TODO = "todo"
    const val CHAPTER_BACKLOG = "chapter_backlog"
    const val PDF_LAUNCHER = "pdf_launcher"

    /** Order a fresh install starts with; matches the layout in the reference design. */
    val DEFAULT_ORDER = listOf(
        CLOCK,
        COUNTDOWN,
        FOCUS_MODE,
        POMODORO,
        TODO,
        CHAPTER_BACKLOG,
        PDF_LAUNCHER
    )
}
