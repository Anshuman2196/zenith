package com.zenith.launcher.data.model

/**
 * Stable string ids for every draggable home-screen widget. Persisted (as an ordered list) in
 * DataStore via [com.zenith.launcher.data.local.PreferencesManager.widgetColumns] so a user's
 * drag-to-reorder arrangement survives app restarts.
 *
 * There is deliberately no standalone "clock" widget: the time and date already live in the
 * [com.zenith.launcher.ui.home.components.GreetingHeader], so a second clock on the grid would
 * just be a duplicate.
 */
object WidgetIds {
    const val COUNTDOWN = "countdown"
    const val FOCUS_MODE = "focus_mode"
    const val POMODORO = "pomodoro"
    const val TODO = "todo"
    const val CHAPTER_BACKLOG = "chapter_backlog"
    const val PDF_LAUNCHER = "pdf_launcher"
    const val MILESTONE = "milestone"
    const val APP_SHORTCUTS = "app_shortcuts"
    const val SYSTEM_STATUS = "system_status"

    /** Order a fresh install starts with; matches the layout in the reference design. */
    val DEFAULT_ORDER = listOf(
        COUNTDOWN,
        FOCUS_MODE,
        MILESTONE,
        POMODORO,
        TODO,
        CHAPTER_BACKLOG,
        PDF_LAUNCHER,
        APP_SHORTCUTS,
        SYSTEM_STATUS
    )

    /**
     * Home screen is laid out as a 3-column masonry grid (matching the reference design), so the
     * persisted arrangement is a list of *columns*, each an ordered list of widget ids, rather
     * than one flat list. This is the arrangement a fresh install starts with.
     */
    /** Initial heights (dp) tuned to the reference home-screen proportions. */
    val DEFAULT_HEIGHTS: Map<String, Int> = mapOf(
        COUNTDOWN to 112,
        FOCUS_MODE to 112,
        MILESTONE to 190,
        POMODORO to 430,
        SYSTEM_STATUS to 150,
        TODO to 190,
        CHAPTER_BACKLOG to 172,
        PDF_LAUNCHER to 150,
        APP_SHORTCUTS to 132
    )

    val DEFAULT_COLUMNS: List<List<String>> = listOf(
        // Left column
        listOf(COUNTDOWN, FOCUS_MODE, MILESTONE, SYSTEM_STATUS),

        // Middle column
        listOf(POMODORO, APP_SHORTCUTS),

        // Right column
        listOf(TODO, CHAPTER_BACKLOG, PDF_LAUNCHER)
    )
}
