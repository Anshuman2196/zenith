package com.zenith.launcher.data.model
import kotlinx.serialization.Serializable


/** Vertical size presets for Home widgets; retained independently of their grid position. */
@Serializable
enum class WidgetSize(val minHeightDp: Int) {
    COMPACT(96),
    STANDARD(160),
    EXPANDED(240);

    fun next(): WidgetSize = entries[(ordinal + 1) % entries.size]
}
