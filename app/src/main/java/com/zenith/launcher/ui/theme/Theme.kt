package com.zenith.launcher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.zenith.launcher.data.model.FontChoice
import java.io.File

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
 * [fontChoice] is likewise a Settings toggle (Settings > Font) and re-skins every piece of text
 * in the app, including the Home screen greeting.
 */
@Composable
fun ZenithStudyHelperTheme(
    darkTheme: Boolean,
    fontChoice: FontChoice = FontChoice.DEFAULT,
    customFontPath: String? = null,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = appTypography(customFontPath?.let(::fontFamilyFromPath) ?: fontChoice.toFontFamily()),
        content = content
    )
}

private fun fontFamilyFromPath(path: String): androidx.compose.ui.text.font.FontFamily? = runCatching {
    if (File(path).isFile) androidx.compose.ui.text.font.FontFamily(android.graphics.Typeface.createFromFile(path)) else null
}.getOrNull()
