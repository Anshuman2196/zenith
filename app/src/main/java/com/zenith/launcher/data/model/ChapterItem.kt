package com.zenith.launcher.data.model

import kotlinx.serialization.Serializable

/**
 * How urgently a backlog chapter needs attention - drives both its color-coding and its sort
 * position in the widget (higher urgency sorts to the top). Enum declaration order matters here:
 * [ChapterUrgency.ordinal] is used directly for the top-first sort.
 */
@Serializable
enum class ChapterUrgency { LOW, MEDIUM, HIGH }

/** One chapter/topic tracked in the "Backlog" widget. */
@Serializable
data class ChapterItem(
    val id: String,
    val subject: String,       // e.g. "Physics", "Chemistry", "Maths"
    val chapterName: String,   // e.g. "Rotational Mechanics"
    val urgency: ChapterUrgency = ChapterUrgency.MEDIUM
)
