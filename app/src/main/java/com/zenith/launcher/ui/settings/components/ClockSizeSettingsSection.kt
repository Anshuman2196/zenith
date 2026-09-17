package com.zenith.launcher.ui.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.RadioButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zenith.launcher.data.model.ClockSize

/** Lets the user control the Home header clock independently of phone/tablet dimensions. */
@Composable
fun ClockSizeSettingsSection(selected: ClockSize, onSelect: (ClockSize) -> Unit) {
    SettingsSectionCard(title = "Clock size") {
        Text(
            "Choose how large the time appears in the Home header.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column {
            ClockSize.entries.forEach { size ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(selected = size == selected, onClick = { onSelect(size) }),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = size == selected, onClick = { onSelect(size) })
                    Text(size.label, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}
