package com.zenith.launcher.ui.settings.components
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier


/** Theme Toggle: Light <-> Dark. Dark mode is the default - eye-friendly for night study. */
@Composable
fun ThemeToggleSection(isDarkMode: Boolean, onToggle: (Boolean) -> Unit) {
    SettingsSectionCard(title = "Appearance") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(if (isDarkMode) "Dark mode (eye-friendly for night study)" else "Light mode", style = MaterialTheme.typography.bodyLarge)
            Switch(checked = isDarkMode, onCheckedChange = onToggle)
        }
    }
}
