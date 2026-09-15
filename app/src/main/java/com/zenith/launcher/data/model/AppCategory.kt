package com.zenith.launcher.data.model

/**
 * How the App Drawer groups installed apps (see [com.zenith.launcher.ui.home.components.AppDrawerOverlay]).
 * New apps receive a sensible package/label-based first suggestion, which users can re-assign
 * at any time via long-press > "Move to..." in the drawer or Settings.
 */
enum class AppCategory {
    STUDY,
    GAMES,
    SOCIAL,
    ENTERTAINMENT,
    OTHER;

    companion object {
        fun fromStorageValue(value: String?): AppCategory = entries.firstOrNull { it.name == value } ?: OTHER
    }
}

val AppCategory.displayName: String
    get() = when (this) {
        AppCategory.STUDY -> "Study"
        AppCategory.GAMES -> "Games"
        AppCategory.SOCIAL -> "Social"
        AppCategory.ENTERTAINMENT -> "Entertainment"
        AppCategory.OTHER -> "Other"
    }

/** Built-in category labels. Users may add to these through Settings. */
val defaultAppCategoryTypes: List<String> = AppCategory.entries.map { it.displayName }
