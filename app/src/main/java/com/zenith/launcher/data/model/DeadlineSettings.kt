package com.zenith.launcher.data.model
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName


/**
 * One deadline the user is tracking - fully user-defined (name + target date). [dateMillis] is epoch millis UTC midnight of that date (matches
 * what Compose Material3's DatePicker returns), or null if the date isn't set yet.
 */
@Serializable
data class DeadlineTarget(
    val id: String,
    val name: String,
    val dateMillis: Long? = null
)

/** Every deadline the user is tracking, shown by the Home screen's Deadlines widget. */
@Serializable
data class DeadlineSettings(
    @SerialName("exams") val deadlines: List<DeadlineTarget> = emptyList()
)
