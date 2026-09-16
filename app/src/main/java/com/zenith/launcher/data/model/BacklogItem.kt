package com.zenith.launcher.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/**
 * How urgently a backlog item needs attention - drives both its color-coding and its sort
 * position in the widget (higher urgency sorts to the top). Enum declaration order matters here:
 * [BacklogUrgency.ordinal] is used directly for the top-first sort.
 */
@Serializable
enum class BacklogUrgency { LOW, MEDIUM, HIGH }

/** One topic/chapter tracked in the "Backlog" widget. */
@Serializable
data class BacklogItem(
    val id: String,
    val subject: String,       // e.g. "Physics", "Chemistry", "Mathematics"
    @SerialName("chapterName") val itemName: String,   // e.g. "Rotational Mechanics"
    val urgency: BacklogUrgency = BacklogUrgency.MEDIUM
)
