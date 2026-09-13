package com.zenith.launcher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = CalmBlue80,
    secondary = SoftTeal80,
    tertiary = WarmNeutral80,
    background = DarkBackground,
    surface = DarkSurface,
)

private val LightColors = lightColorScheme(
    primary = CalmBlue40,
    secondary = SoftTeal40,
    tertiary = WarmNeutral40,
    background = LightBackground,
    surface = LightSurface,
)

/**
 * App-wide Material3 theme. [darkTheme] is driven by the Settings toggle (persisted in
 * DataStore), not the system setting - predictable regardless of the phone's auto dark schedule.
 */
@Composable
fun ZenithLauncherTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content
    )
}
