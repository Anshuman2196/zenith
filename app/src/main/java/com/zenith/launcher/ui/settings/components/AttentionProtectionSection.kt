package com.zenith.launcher.ui.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.zenith.launcher.data.model.AttentionProtectionMode

/** Chooses how much Zenith can reduce access to system-level distractions during protected moments. */
@Composable
fun AttentionProtectionSection(
    selected: AttentionProtectionMode,
    onSelect: (AttentionProtectionMode) -> Unit
) {
    SettingsSectionCard(title = "Attention protection") {
        Text(
            "Give Focus Mode and short reflection windows enough space to work. Android controls what a normal app can hide; Dedicated device uses Lock Task only when the phone is provisioned for it.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        AttentionProtectionMode.entries.forEach { mode ->
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                RadioButton(selected = selected == mode, onClick = { onSelect(mode) })
                Column {
                    Text(mode.title, style = MaterialTheme.typography.bodyMedium)
                    Text(mode.description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
