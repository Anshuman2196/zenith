package com.zenith.launcher.data.model
import androidx.compose.runtime.getValue

/**
 * The type faces the user can pick for the whole launcher UI from Settings > Font. Kept as a
 * plain enum here (no Compose dependency) - [com.zenith.launcher.ui.theme] maps each value to an
 * actual `FontFamily` so the data layer stays UI-framework-agnostic.
 */
enum class FontChoice {
    CLASSIC,
    ELEGANT,
    PLAYFUL,
    TECHNICAL;

    companion object {
        val DEFAULT = CLASSIC

        /** Tolerant parse for values persisted by a previous app version. */
        fun fromStorageValue(value: String?): FontChoice =
            entries.firstOrNull { it.name == value } ?: DEFAULT
    }
}
