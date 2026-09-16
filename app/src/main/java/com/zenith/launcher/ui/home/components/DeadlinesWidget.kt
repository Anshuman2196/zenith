package com.zenith.launcher.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zenith.launcher.ui.home.DeadlineCountdown
import com.zenith.launcher.ui.home.ZenithCopy

/**
 * Widget: Deadlines - live "days remaining" for every deadline the user has added (see
 * Settings > Deadlines; there's no fixed deadline list, this simply reflects whatever's there).
 * Null [DeadlineCountdown.daysLeft] means that deadline's date hasn't been set yet.
 */
@Composable
fun DeadlinesWidget(deadlines: List<DeadlineCountdown>) {
    WidgetCard {
        Text("Deadlines", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))

        if (deadlines.isEmpty()) {
            Text(
                ZenithCopy.emptyDeadlines[java.time.LocalDate.now().dayOfYear % ZenithCopy.emptyDeadlines.size],
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Two per row keeps this widget from growing unbounded tall with many deadlines,
                // while still reading naturally left-to-right for the common 1-3 deadline case.
                deadlines.chunked(2).forEach { rowOfDeadlines ->
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        rowOfDeadlines.forEach { deadline -> DeadlineColumn(deadline) }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeadlineColumn(deadline: DeadlineCountdown) {
    Column(modifier = Modifier.weight(1f)) {
        Text(
            text = deadline.daysLeft?.toString() ?: "--",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = if (deadline.daysLeft == null) "${deadline.name} - set date in Settings" else "days until ${deadline.name}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
