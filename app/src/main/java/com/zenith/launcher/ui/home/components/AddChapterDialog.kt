package com.zenith.launcher.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
import com.zenith.launcher.data.model.ChapterStatus

/** Dialog for adding one item to the Chapter Backlog widget. */
@Composable
fun AddChapterDialog(
    onDismiss: () -> Unit,
    onConfirm: (subject: String, chapter: String, status: ChapterStatus) -> Unit
) {
    var subject by remember { mutableStateOf("") }
    var chapter by remember { mutableStateOf("") }
    var status by remember { mutableStateOf(ChapterStatus.PENDING) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add chapter") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = subject, onValueChange = { subject = it },
                    label = { Text("Subject (e.g. Physics)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = chapter, onValueChange = { chapter = it },
                    label = { Text("Chapter name") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Status", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChapterStatus.entries.forEach { option ->
                        FilterChip(
                            selected = status == option,
                            onClick = { status = option },
                            label = { Text(option.name.lowercase().replace('_', ' ')) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(subject.ifBlank { "General" }, chapter, status) },
                enabled = chapter.isNotBlank()
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
