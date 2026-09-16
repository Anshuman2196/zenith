package com.zenith.launcher.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zenith.launcher.data.model.MilestoneTarget
import com.zenith.launcher.ui.home.LauncherCopy

/**
 * Widget: "Targets" - one or more self-set goals for upcoming study goals, each with a
 * target score and (optionally) the most recent actual score. Tap + to add another target, the
 * pencil on a row to edit it, or the trash icon to remove it.
 */
@Composable
fun MilestoneWidget(
    targets: List<MilestoneTarget>,
    onAdd: () -> Unit,
    onChange: (MilestoneTarget) -> Unit,
    onDelete: (String) -> Unit
) {
    var editingTarget by remember { mutableStateOf<MilestoneTarget?>(null) }

    WidgetCard {
        WidgetHeaderRow(title = "Targets", onAddClick = onAdd)

        if (targets.isEmpty()) {
            EmptyHint(LauncherCopy.emptyMilestones[java.time.LocalDate.now().dayOfYear % LauncherCopy.emptyMilestones.size])
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(targets, key = { it.id }) { target ->
                    MilestoneRow(
                        target = target,
                        onEdit = { editingTarget = target },
                        onDelete = { onDelete(target.id) }
                    )
                }
            }
        }
    }

    editingTarget?.let { target ->
        EditMilestoneDialog(
            target = target,
            onDismiss = { editingTarget = null },
            onConfirm = { onChange(it); editingTarget = null }
        )
    }
}

@Composable
private fun MilestoneRow(target: MilestoneTarget, onEdit: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(target.testName, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            Text(
                "Target: ${target.targetScore}/${target.maxScore}" + (target.lastScore?.let { " · Last: $it" } ?: ""),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row {
            IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Edit, contentDescription = "Edit ${target.testName}", modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = "Remove ${target.testName}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun EditMilestoneDialog(
    target: MilestoneTarget,
    onDismiss: () -> Unit,
    onConfirm: (MilestoneTarget) -> Unit
) {
    var testName by remember { mutableStateOf(target.testName) }
    var targetScore by remember { mutableStateOf(target.targetScore.toString()) }
    var maxScore by remember { mutableStateOf(target.maxScore.toString()) }
    var lastScore by remember { mutableStateOf(target.lastScore?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit target") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = testName, onValueChange = { testName = it },
                    label = { Text("Target name") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = targetScore, onValueChange = { targetScore = it.filter(Char::isDigit) },
                    label = { Text("Target score") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = maxScore, onValueChange = { maxScore = it.filter(Char::isDigit) },
                    label = { Text("Max possible score") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = lastScore, onValueChange = { lastScore = it.filter(Char::isDigit) },
                    label = { Text("Last score (optional)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(
                    target.copy(
                        testName = testName.ifBlank { "Study target" },
                        targetScore = targetScore.toIntOrNull() ?: target.targetScore,
                        maxScore = maxScore.toIntOrNull() ?: target.maxScore,
                        lastScore = lastScore.toIntOrNull()
                    )
                )
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
