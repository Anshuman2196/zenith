package com.zenith.launcher.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zenith.launcher.data.model.MilestoneTarget

/**
 * Widget: "Milestone Target" - the aspirant's self-set goal for their next big mock test. Shows
 * the test name and target score. Tap the pencil to edit them.
 */
@Composable
fun MilestoneWidget(target: MilestoneTarget, onChange: (MilestoneTarget) -> Unit) {
    var showEditDialog by remember { mutableStateOf(false) }
    WidgetCard {
        MilestoneHeaderRow(target = target, onEditClick = { showEditDialog = true })
        Spacer(Modifier.height(4.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(
                target.testName,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Text(
                "Target: ${target.targetScore}/${target.maxScore}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }

    if (showEditDialog) {
        EditMilestoneDialog(
            target = target,
            onDismiss = { showEditDialog = false },
            onConfirm = { onChange(it); showEditDialog = false }
        )
    }
}

@Composable
private fun MilestoneHeaderRow(target: MilestoneTarget, onEditClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Milestone Target", style = MaterialTheme.typography.titleMedium)
        IconButton(onClick = onEditClick, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Edit, contentDescription = "Edit milestone target", modifier = Modifier.size(16.dp))
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit milestone target") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = testName, onValueChange = { testName = it },
                    label = { Text("Test name") }, singleLine = true,
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
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(
                    target.copy(
                        testName = testName.ifBlank { "Comprehensive Mock Test" },
                        targetScore = targetScore.toIntOrNull() ?: target.targetScore,
                        maxScore = maxScore.toIntOrNull() ?: target.maxScore,
                    )
                )
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
