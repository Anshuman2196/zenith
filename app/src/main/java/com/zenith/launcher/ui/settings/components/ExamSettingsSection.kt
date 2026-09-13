package com.zenith.launcher.ui.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import com.zenith.launcher.data.model.ExamSettings
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** Exam Settings: set/modify the target dates that drive the Home screen's Countdown widget. */
@Composable
fun ExamSettingsSection(
    examSettings: ExamSettings,
    onJeeMainDateChange: (Long?) -> Unit,
    onJeeAdvancedDateChange: (Long?) -> Unit
) {
    SettingsSectionCard(title = "Exam Dates") {
        ExamDateRow(label = "JEE Main", dateMillis = examSettings.jeeMainDateMillis, onDateChange = onJeeMainDateChange)
        Spacer(Modifier.height(12.dp))
        ExamDateRow(label = "JEE Advanced", dateMillis = examSettings.jeeAdvancedDateMillis, onDateChange = onJeeAdvancedDateChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExamDateRow(label: String, dateMillis: Long?, onDateChange: (Long?) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }
    val formatter = remember { DateTimeFormatter.ofPattern("dd MMM yyyy") }
    val displayText = dateMillis?.let {
        Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().format(formatter)
    } ?: "Not set"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(displayText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextButton(onClick = { showPicker = true }) { Text(if (dateMillis == null) "Set date" else "Change") }
    }

    if (showPicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = dateMillis)
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
