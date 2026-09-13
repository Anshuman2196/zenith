package com.zenith.launcher.data.model

/**
 * The two JEE exam target dates, stored as epoch millis (UTC midnight of that date - matches
 * what Compose Material3's DatePicker returns). Null means the user hasn't set that exam yet.
 */
data class ExamSettings(
    val jeeMainDateMillis: Long? = null,
    val jeeAdvancedDateMillis: Long? = null
)
