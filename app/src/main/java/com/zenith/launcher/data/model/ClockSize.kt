package com.zenith.launcher.data.model

/** User-controlled size for the live Home header clock. */
enum class ClockSize(val label: String) {
    SMALL("Small"),
    MEDIUM("Medium"),
    LARGE("Large");

    companion object {
        fun fromStorageValue(value: String?): ClockSize =
            entries.firstOrNull { it.name == value } ?: LARGE
    }
}
