package com.zenith.launcher.ui.settings.components
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zenith.launcher.data.model.IconPackInfo


/**
 * Icon Pack Picker: choose which installed third-party icon pack re-skins the app grid.
 * "System default" always shows first so the user can revert easily.
 */
@Composable
fun IconPackPickerSection(
    availablePacks: List<IconPackInfo>,
    selectedPackage: String?,
    onSelect: (String?) -> Unit
) {
    SettingsSectionCard(title = "Icon Pack") {
        if (availablePacks.isEmpty()) {
            Text(
                "No third-party icon packs installed. Install one from the Play Store to enable this.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column {
            IconPackRow(label = "System default", isSelected = selectedPackage == null, onClick = { onSelect(null) })
            availablePacks.forEach { pack ->
                IconPackRow(
                    label = pack.label,
                    isSelected = selectedPackage == pack.packageName,
                    onClick = { onSelect(pack.packageName) }
                )
            }
        }
    }
}

@Composable
private fun IconPackRow(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = isSelected, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RadioButton(selected = isSelected, onClick = onClick)
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}
