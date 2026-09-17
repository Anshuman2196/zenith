package com.zenith.launcher.data.model
import kotlinx.serialization.Serializable


/**
 * A stable reference to one app pinned to the Home screen's "App Shortcuts" widget. Only the
 * component identity is persisted (not the icon/label - those are live data resolved each time
 * from [com.zenith.launcher.data.repository.AppRepository] so a shortcut always reflects the
 * app's current icon pack skin and label, and disappears cleanly if the app is uninstalled).
 */
@Serializable
data class AppShortcutRef(
    val packageName: String,
    val activityClassName: String
)
