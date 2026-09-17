package com.zenith.launcher.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zenith.launcher.data.model.BacklogItem
import com.zenith.launcher.data.model.BacklogUrgency
import com.zenith.launcher.data.model.TodoItem

@Composable
fun EditTodoDialog(item: TodoItem, onDismiss: () -> Unit, onConfirm: (TodoItem) -> Unit) {
    var text by remember(item.id) { mutableStateOf(item.text) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit task") },
        text = {
            OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Task") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        },
        confirmButton = { TextButton(onClick = { onConfirm(item.copy(text = text.trim())) }, enabled = text.isNotBlank()) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun EditBacklogDialog(item: BacklogItem, onDismiss: () -> Unit, onConfirm: (BacklogItem) -> Unit) {
    var subject by remember(item.id) { mutableStateOf(item.subject) }
    var itemName by remember(item.id) { mutableStateOf(item.itemName) }
    var urgency by remember(item.id) { mutableStateOf(item.urgency) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit backlog item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("Subject") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = itemName, onValueChange = { itemName = it }, label = { Text("Topic or chapter") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BacklogUrgency.entries.forEach { option ->
                        FilterChip(selected = urgency == option, onClick = { urgency = option }, label = { Text(option.name.lowercase().replaceFirstChar(Char::uppercase)) })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(item.copy(subject = subject.trim().ifBlank { "General" }, itemName = itemName.trim(), urgency = urgency)) }, enabled = itemName.isNotBlank()) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
