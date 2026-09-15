package com.zenith.launcher.data.model

import kotlinx.serialization.Serializable

/**
 * One exam the user is preparing for - fully user-defined (name + target date), rather than a
 * fixed JEE Main/Advanced pair. [dateMillis] is epoch millis UTC midnight of that date (matches
 * what Compose Material3's DatePicker returns), or null if the date isn't set yet.
 */
@Serializable
data class ExamTarget(
    val id: String,
    val name: String,
    val dateMillis: Long? = null
)

/** Every exam the user is tracking, shown by the Home screen's Exam Countdown widget. */
@Serializable
data class ExamSettings(val exams: List<ExamTarget> = emptyList())
