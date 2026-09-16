package com.zenith.launcher.ui.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zenith.launcher.data.model.DeadlineSettings
import com.zenith.launcher.data.model.DeadlineTarget
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Deadline settings: a fully user-managed list of deadlines (no fixed set of deadlines) -
 * add as many as needed, rename them, set or change each target date, or remove one. Every
 * entry here shows up as a row in the Home screen's Deadlines widget.
 */
@Composable
fun DeadlineSettingsSection(deadlineSettings: DeadlineSettings, onChange: (DeadlineSettings) -> Unit) {
    var showAddDialog by remember { mutableStateOf(false) }

    SettingsSectionCard(title = "Deadlines") {
        if (deadlineSettings.deadlines.isEmpty()) {
            Text(
                "No deadlines yet - add one below.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            deadlineSettings.deadlines.forEach { deadline ->
                DeadlineRow(
                    deadline = deadline,
                    onDateChange = { newDate ->
                        onChange(deadlineSettings.copy(deadlines = deadlineSettings.deadlines.map { if (it.id == deadline.id) it.copy(dateMillis = newDate) else it }))
                    },
                    onRemove = {
                        onChange(deadlineSettings.copy(deadlines = deadlineSettings.deadlines.filterNot { it.id == deadline.id }))
                    }
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add deadline")
            }
        }
    }

    if (showAddDialog) {
        AddDeadlineDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name ->
                onChange(deadlineSettings.copy(deadlines = deadlineSettings.deadlines + DeadlineTarget(id = UUID.randomUUID().toString(), name = name)))
                showAddDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeadlineRow(deadline: DeadlineTarget, onDateChange: (Long?) -> Unit, onRemove: () -> Unit) {
    var showPicker by remember { mutableStateOf(false) }
    val formatter = remember { DateTimeFormatter.ofPattern("dd MMM yyyy") }
    val displayText = deadline.dateMillis?.let {
        Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().format(formatter)
    } ?: "Not set"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(deadline.name, style = MaterialTheme.typography.bodyLarge)
            Text(displayText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { showPicker = true }) { Text(if (deadline.dateMillis == null) "Set date" else "Change") }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Remove ${deadline.name}")
            }
        }
    }

    if (showPicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = deadline.dateMillis)
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onDateChange(pickerState.selectedDateMillis)
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun AddDeadlineDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add deadline") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Deadline name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onConfirm(name.trim()) }, enabled = name.isNotBlank()) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
