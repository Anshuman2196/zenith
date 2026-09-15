package com.zenith.launcher.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.zenith.launcher.data.model.TodoItem

/** Widget 3: Daily To-Do List - a quick checklist for the day's study goals. */
@Composable
fun TodoWidget(
    items: List<TodoItem>,
    onAddClick: () -> Unit,
    onToggle: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    WidgetCard {
        WidgetHeaderRow(title = "Today's To-Do", onAddClick = onAddClick)

        if (items.isEmpty()) {
            EmptyHint("No tasks yet - tap + to add one")
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
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
                            Checkbox(
                                checked = item.isDone,
                                onCheckedChange = { onToggle(item.id) }
                            )
                            Text(
                                text = item.text,
                                style = MaterialTheme.typography.bodyMedium,
                                textDecoration = if (item.isDone) TextDecoration.LineThrough else null,
                                color = if (item.isDone) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                        IconButton(onClick = { onDelete(item.id) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete task",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Small "Title ..... [+]" row reused by every list-style widget for a consistent look. */
@Composable
fun WidgetHeaderRow(title: String, onAddClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        IconButton(onClick = onAddClick) {
            Icon(Icons.Default.Add, contentDescription = "Add", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun EmptyHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}
