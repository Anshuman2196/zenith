package com.zenith.launcher.data.model

/**
 * The user's chosen home-screen background. At most one of the two is "active" at a time:
 * an image takes priority over a solid color when both happen to be set. When both are null,
 * Home falls back to the current Material theme's background color.
 */
data class BackgroundSettings(
    val imageUri: String? = null,
    val colorArgb: Long? = null
)
