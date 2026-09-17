package com.zenith.launcher.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zenith.launcher.ui.home.DeadlineCountdown
import com.zenith.launcher.ui.home.ZenithCopy

/** Shows the three nearest deadlines; the overflow button opens the complete list. */
@Composable
fun DeadlinesWidget(deadlines: List<DeadlineCountdown>, onAddClick: () -> Unit) {
    var showAll by remember { mutableStateOf(false) }
    val visible = remember(deadlines) {
        deadlines.sortedWith(compareBy({ it.daysLeft == null }, { it.daysLeft ?: Long.MAX_VALUE }, { it.name.lowercase() }))
            .take(3)
    }
    val overflow = (deadlines.size - visible.size).coerceAtLeast(0)

    WidgetCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Deadlines", style = MaterialTheme.typography.titleMedium)
                if (overflow > 0) {
                    Text("$overflow more", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (overflow > 0) {
                    IconButton(onClick = { showAll = true }) {
                        Icon(Icons.Default.MoreHoriz, contentDescription = "View all deadlines")
                    }
                }
                TextButton(onClick = onAddClick) { Text("Add") }
            }
        }
        Spacer(Modifier.height(6.dp))
        if (visible.isEmpty()) {
            Text(
                ZenithCopy.emptyDeadlines.random(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                visible.forEach { DeadlineRow(it) }
            }
        }
    }

    if (showAll) {
        AlertDialog(
            onDismissRequest = { showAll = false },
            title = { Text("All deadlines") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    deadlines.sortedWith(compareBy({ it.daysLeft == null }, { it.daysLeft ?: Long.MAX_VALUE }, { it.name.lowercase() }))
                        .forEach { DeadlineRow(it) }
                }
            },
            confirmButton = { TextButton(onClick = { showAll = false }) { Text("Close") } }
        )
    }
}

@Composable
private fun DeadlineRow(deadline: DeadlineCountdown) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            deadline.daysLeft?.toString() ?: "--",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            if (deadline.daysLeft == null) "${deadline.name} · date not set" else "${deadline.name} · days left",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}
