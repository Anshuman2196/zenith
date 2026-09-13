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
import com.zenith.launcher.data.model.FontChoice
import com.zenith.launcher.ui.theme.displayName
import com.zenith.launcher.ui.theme.toFontFamily

/**
 * Font picker: re-skins every piece of text in the launcher, including the Home screen greeting.
 * Each row is rendered in its own type face so the user sees a live preview before picking it.
 */
@Composable
fun FontSettingsSection(selected: FontChoice, onSelect: (FontChoice) -> Unit) {
    SettingsSectionCard(title = "Font") {
        Text(
            "Changes the type face used across the whole launcher, including the greeting.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column {
            FontChoice.entries.forEach { choice ->
                FontRow(choice = choice, isSelected = choice == selected, onClick = { onSelect(choice) })
            }
        }
    }
}

@Composable
private fun FontRow(choice: FontChoice, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = isSelected, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RadioButton(selected = isSelected, onClick = onClick)
        Text(
            choice.displayName,
            style = MaterialTheme.typography.bodyLarge.copy(fontFamily = choice.toFontFamily())
        )
    }
}
