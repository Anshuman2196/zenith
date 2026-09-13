package com.zenith.launcher.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.zenith.launcher.data.model.FontChoice

/**
 * Deliberately restrained type scale - a calm launcher shouldn't shout - parameterized by
 * [fontFamily] so the whole app can be re-skinned from Settings > Font (see [FontChoice]).
 *
 * The greeting on Home reads [Typography.headlineMedium] directly and is sized a little larger
 * and lighter than a typical Material headline so it reads closer to the warm, editorial feel of
 * the reference design.
 */
fun appTypography(fontFamily: FontFamily): Typography = Typography(
    headlineMedium = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = 0.2.sp
    ),
    titleLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 18.sp),
    labelSmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 11.sp, lineHeight = 14.sp)
)

/** Maps a persisted [FontChoice] to the actual Compose [FontFamily] used to render it. */
fun FontChoice.toFontFamily(): FontFamily = when (this) {
    FontChoice.CLASSIC -> FontFamily.Default
    FontChoice.ELEGANT -> FontFamily.Serif
    FontChoice.PLAYFUL -> FontFamily.Cursive
    FontChoice.TECHNICAL -> FontFamily.Monospace
}

/** Short, user-facing label shown next to each option in Settings > Font. */
val FontChoice.displayName: String
    get() = when (this) {
        FontChoice.CLASSIC -> "Classic"
        FontChoice.ELEGANT -> "Elegant"
        FontChoice.PLAYFUL -> "Playful"
        FontChoice.TECHNICAL -> "Technical"
    }
