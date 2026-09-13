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
 * Widget: Clock. Large time-of-day + full date, ticking forward every second so it never
 * looks stale even if the launcher process has been sitting in the foreground a while.
 */
@Composable
fun ClockWidget(modifier: Modifier = Modifier) {
    var now by remember { mutableStateOf(LocalDateTime.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(1000)
        }
    }

    WidgetCard(modifier = modifier) {
        Column {
            Text(
                text = now.format(TIME_FORMATTER),
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 40.sp),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = now.format(DATE_FORMATTER),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Compact time-only text for the header clock, next to the settings gear. */
@Composable
fun InlineClock(modifier: Modifier = Modifier) {
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
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.End
        )
        Text(
            text = now.format(DATE_FORMATTER_SHORT),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End
        )
    }
}

private val TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a")
private val DATE_FORMATTER = DateTimeFormatter.ofPattern("EEEE, MMM d")
private val DATE_FORMATTER_SHORT = DateTimeFormatter.ofPattern("EEE, MMM d")
