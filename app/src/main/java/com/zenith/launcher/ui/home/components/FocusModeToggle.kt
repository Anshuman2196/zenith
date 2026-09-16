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
import com.zenith.launcher.ui.home.LauncherCopy

/**
 * Widget: Focus Mode Toggle.
 * When ON, HomeViewModel filters the app drawer/app grid down to ONLY the apps the user
 * explicitly allowed in Settings > Focus Mode App Picker (an allow-list, not a block-list).
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
                    text = if (isActive) LauncherCopy.focusMode[0] else LauncherCopy.focusModeOff[0],
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = isActive, onCheckedChange = { onToggle() })
        }
    }
}
