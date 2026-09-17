package com.zenith.launcher.ui.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zenith.launcher.data.model.DeadlineTarget
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Composable
fun DeadlinesSettingsSection(
    deadlines: List<DeadlineTarget>,
    onAdd: () -> Unit,
    onDelete: (String) -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
    SettingsSectionCard(title = "Deadlines") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            if (deadlines.isEmpty()) {
                Text("Add dates for exams, submissions or other fixed milestones.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                deadlines.forEach { deadline ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(deadline.name, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                deadline.dateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().format(formatter) } ?: "Date not set",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { onDelete(deadline.id) }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete ${deadline.name}")
                        }
                    }
                }
            }
            TextButton(onClick = onAdd, modifier = Modifier.align(Alignment.Start)) { Text("Add deadline") }
        }
    }
}
