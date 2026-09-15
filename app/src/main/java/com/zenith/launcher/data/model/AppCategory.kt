package com.zenith.launcher.data.model

/**
 * How the App Drawer groups installed apps (see [com.zenith.launcher.ui.home.components.AppDrawerOverlay]).
 * Every app defaults to [OTHER] until the user re-assigns it - via long-press > "Move to..." in
 * the drawer, or the "App Categories" section in Settings - so categorization is entirely
 * user-driven rather than guessed from package metadata.
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
