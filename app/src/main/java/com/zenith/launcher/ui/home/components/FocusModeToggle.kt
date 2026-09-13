package com.zenith.launcher.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Widget 6: Focus Mode Toggle.
 * When ON, HomeViewModel filters the app grid to hide every package the user marked as
 * "distracting" in Settings > Focus Mode App Picker.
 */
@Composable
fun FocusModeToggle(isActive: Boolean, onToggle: () -> Unit) {
    WidgetCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Focus Mode", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = if (isActive) "Distracting apps are hidden" else "All apps visible",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = isActive, onCheckedChange = { onToggle() })
        }
    }
}
