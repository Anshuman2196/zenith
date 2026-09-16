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
    // Kotlin names use the current product terminology; string values stay stable for saved layouts.
    const val DEADLINES = "countdown"
    const val FOCUS_MODE = "focus_mode"
    const val POMODORO = "pomodoro"
    const val TODO = "todo"
    const val BACKLOG = "chapter_backlog"
    const val LIBRARY = "pdf_launcher"
    const val TARGETS = "milestone"
    const val APP_SHORTCUTS = "app_shortcuts"
    const val SYSTEM_STATUS = "system_status"

    /** Order a fresh install starts with; matches the layout in the reference design. */
    val DEFAULT_ORDER = listOf(
        DEADLINES,
        FOCUS_MODE,
        TARGETS,
        POMODORO,
        TODO,
        BACKLOG,
        LIBRARY,
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
        DEADLINES to 112,
        FOCUS_MODE to 112,
        TARGETS to 190,
        POMODORO to 430,
        SYSTEM_STATUS to 150,
        TODO to 190,
        BACKLOG to 172,
        LIBRARY to 150,
        APP_SHORTCUTS to 132
    )

    val DEFAULT_COLUMNS: List<List<String>> = listOf(
        // Left column
        listOf(DEADLINES, FOCUS_MODE, TARGETS, SYSTEM_STATUS),

        // Middle column
        listOf(POMODORO, APP_SHORTCUTS),

        // Right column
        listOf(TODO, BACKLOG, LIBRARY)
    )
}
