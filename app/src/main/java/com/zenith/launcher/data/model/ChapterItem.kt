package com.zenith.launcher.data.model

import kotlinx.serialization.Serializable

/** Simple status used for color-coding a chapter in the backlog widget. */
@Serializable
enum class ChapterStatus { PENDING, REVISION, WEAK_AREA }

/** One chapter/topic tracked in the "Chapter Backlog" widget. */
@Serializable
data class ChapterItem(
    val id: String,
    val subject: String,       // e.g. "Physics", "Chemistry", "Maths"
    val chapterName: String,   // e.g. "Rotational Mechanics"
    val status: ChapterStatus = ChapterStatus.PENDING
)
