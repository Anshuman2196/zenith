package com.zenith.launcher.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.zenith.launcher.data.model.ChapterItem
import com.zenith.launcher.data.model.ChapterUrgency
import com.zenith.launcher.ui.theme.AccentDanger
import com.zenith.launcher.ui.theme.AccentWarning
import com.zenith.launcher.ui.home.LauncherCopy

/**
 * Widget 4: Chapter Backlog List - pending chapters, revision topics, and weak areas, each tagged
 * with an urgency (Low/Medium/High). Higher-urgency chapters sort to the top of the list, so the
 * thing that most needs attention is always what's visible first without scrolling.
 */
@Composable
fun ChapterBacklogWidget(
    items: List<ChapterItem>,
    onAddClick: () -> Unit,
    onDelete: (String) -> Unit
) {
    WidgetCard {
        WidgetHeaderRow(title = "Backlog", onAddClick = onAddClick)

        if (items.isEmpty()) {
            EmptyHint(LauncherCopy.emptyChapters[java.time.LocalDate.now().dayOfYear % LauncherCopy.emptyChapters.size])
        } else {
            val sorted = items.sortedByDescending { it.urgency.ordinal }
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(sorted, key = { it.id }) { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            UrgencyDot(item.urgency)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    "${item.subject} - ${item.chapterName}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    item.urgency.label(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        // Deliberately understated - a full-strength destructive icon here would
                        // outweigh the actual chapter text it sits next to in a dense list.
                        IconButton(onClick = { onDelete(item.id) }, modifier = Modifier.size(36.dp)) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = "Remove ${item.chapterName}",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UrgencyDot(urgency: ChapterUrgency) {
    val color = when (urgency) {
        ChapterUrgency.LOW -> MaterialTheme.colorScheme.onSurfaceVariant
        ChapterUrgency.MEDIUM -> AccentWarning
        ChapterUrgency.HIGH -> AccentDanger
    }
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(RoundedCornerShape(50))
            .background(color)
    )
}

private fun ChapterUrgency.label(): String = when (this) {
    ChapterUrgency.LOW -> "Low urgency"
    ChapterUrgency.MEDIUM -> "Medium urgency"
    ChapterUrgency.HIGH -> "High urgency"
}
