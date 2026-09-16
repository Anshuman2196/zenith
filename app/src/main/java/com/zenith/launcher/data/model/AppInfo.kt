package com.zenith.launcher.data.model

import android.content.pm.ApplicationInfo
import android.graphics.drawable.Drawable

/**
 * Represents one installed app shown in Zenith's app grid.
 *
 * @param icon Either the app's own icon, or a themed replacement from the
 *             user-selected icon pack (see [com.zenith.launcher.util.IconPackHelper]).
 */
data class AppInfo(
    val packageName: String,
    val activityClassName: String,
    val label: String,
    val icon: Drawable,
    val isSystemApp: Boolean = false,
    /** Android's coarse app category, used only as a fallback for automatic drawer grouping. */
    val androidCategory: Int = ApplicationInfo.CATEGORY_UNDEFINED
)
