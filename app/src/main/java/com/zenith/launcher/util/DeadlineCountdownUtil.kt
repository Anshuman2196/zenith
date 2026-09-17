package com.zenith.launcher.util
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit


/** Pure date-math helper for the Deadlines widget. */
object DeadlineCountdownUtil {

    /**
     * Days remaining from today until [targetEpochMillis], clamped to 0 once the date passes.
     * Both sides are compared as UTC calendar dates because Compose Material3's DatePicker
     * returns `selectedDateMillis` as UTC midnight of the picked date.
     */
    fun daysRemaining(targetEpochMillis: Long): Long {
        val today = Instant.now().atZone(ZoneOffset.UTC).toLocalDate()
        val target = Instant.ofEpochMilli(targetEpochMillis).atZone(ZoneOffset.UTC).toLocalDate()
        val days = ChronoUnit.DAYS.between(today, target)
        return if (days < 0) 0 else days
    }
}
