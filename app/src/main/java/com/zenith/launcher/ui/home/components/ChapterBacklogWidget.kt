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
import androidx.compose.material.icons.filled.Delete
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
import com.zenith.launcher.data.model.ChapterStatus
import com.zenith.launcher.ui.theme.AccentDanger
import com.zenith.launcher.ui.theme.AccentWarning

/** Widget 4: Chapter Backlog List - pending chapters, revision topics, and weak areas. */
@Composable
fun ChapterBacklogWidget(
    items: List<ChapterItem>,
    onAddClick: () -> Unit,
    onDelete: (String) -> Unit
) {
    WidgetCard {
        WidgetHeaderRow(title = "Chapter Backlog", onAddClick = onAddClick)

        if (items.isEmpty()) {
            EmptyHint("No chapters tracked yet - tap + to add one")
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            StatusDot(item.status)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    "${item.subject} - ${item.chapterName}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    item.status.label(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(onClick = { onDelete(item.id) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Remove",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusDot(status: ChapterStatus) {
    val color = when (status) {
        ChapterStatus.PENDING -> AccentWarning
        ChapterStatus.REVISION -> MaterialTheme.colorScheme.primary
        ChapterStatus.WEAK_AREA -> AccentDanger
    }
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(RoundedCornerShape(50))
            .background(color)
    )
}

private fun ChapterStatus.label(): String = when (this) {
    ChapterStatus.PENDING -> "Pending"
    ChapterStatus.REVISION -> "Needs revision"
    ChapterStatus.WEAK_AREA -> "Weak area"
}
