package com.zenith.launcher.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Top header, matching the reference design: the aspirant's name is centered across the full
 * width of the screen (not just the leftover space next to the clock), while the live clock +
 * date and the gear icon into Settings sit pinned to the top-right corner as their own group.
 *
 * [overPhotoBackground] should be true whenever Home is showing a user photo behind it (see
 * [com.zenith.launcher.ui.home.HomeScreen]). A photo's brightness has nothing to do with whether
 * Light or Dark theme is selected, so instead of using the theme's `onBackground` color (which
 * can turn near-invisible - dark text on a dark photo, in Light theme) this header always renders
 * in a fixed light color with a soft shadow over a photo, matching how the darkening scrim behind
 * it is applied regardless of theme.
 */
@Composable
fun GreetingHeader(
    greeting: String,
    onSettingsClick: () -> Unit,
    overPhotoBackground: Boolean = false
) {
    val textColor = if (overPhotoBackground) Color.White else MaterialTheme.colorScheme.onBackground
    val secondaryTextColor = if (overPhotoBackground) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
    val textShadow = if (overPhotoBackground) {
        Shadow(color = Color.Black.copy(alpha = 0.6f), blurRadius = 12f)
    } else {
        null
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, start = 20.dp, end = 8.dp, bottom = 4.dp)
    ) {
        Text(
            text = greeting,
            style = MaterialTheme.typography.headlineMedium.copy(shadow = textShadow),
            color = textColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                // Reserves roughly the width of the clock + gear group on the right so a long
                // greeting phrase truncates with an ellipsis instead of drawing underneath it.
                .padding(end = 112.dp)
        )

        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            InlineClock(timeColor = textColor, dateColor = secondaryTextColor)
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Launcher settings",
                    tint = textColor
                )
            }
        }
    }
}
