package com.zenith.launcher.ui.home.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Compact time + date text shown inline in [GreetingHeader], next to the settings gear - this is
 * the launcher's only clock (there is no separate clock widget on the grid). Ticks forward every
 * second so it never looks stale even if the launcher process has been sitting in the foreground
 * a while.
 *
 * [timeColor]/[dateColor] default to the theme's on-background colors, but [GreetingHeader]
 * overrides them with a fixed light color when a photo background is active, since a photo's
 * brightness has nothing to do with whether Light or Dark theme is selected.
 */
@Composable
fun InlineClock(
    modifier: Modifier = Modifier,
    timeColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onBackground,
    dateColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    var now by remember { mutableStateOf(LocalDateTime.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(1000)
        }
    }

    Column(modifier = modifier) {
        Text(
            text = now.format(TIME_FORMATTER),
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 28.sp, lineHeight = 32.sp),
            color = timeColor,
            textAlign = TextAlign.End
        )
        Text(
            text = now.format(DATE_FORMATTER_SHORT),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 14.sp, lineHeight = 18.sp),
            color = dateColor,
            textAlign = TextAlign.End
        )
    }
}

private val TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a")
private val DATE_FORMATTER_SHORT = DateTimeFormatter.ofPattern("EEE, MMM d")
