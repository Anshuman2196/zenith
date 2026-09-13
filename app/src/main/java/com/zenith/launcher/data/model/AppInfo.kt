package com.zenith.launcher.data.model

import android.graphics.drawable.Drawable

/**
 * Represents one installed app shown in the launcher's app grid.
 *
 * @param icon Either the app's own launcher icon, or a themed replacement from the
 *             user-selected icon pack (see [com.zenith.launcher.util.IconPackHelper]).
 */
data class AppInfo(
    val packageName: String,
    val activityClassName: String,
    val label: String,
    val icon: Drawable,
    val isSystemApp: Boolean = false
)
