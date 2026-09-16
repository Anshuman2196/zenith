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
import com.zenith.launcher.data.model.WidgetVisibility

/** Widget Visibility Manager: individual ON/OFF toggle for every home-screen widget. */
@Composable
fun WidgetVisibilitySection(
    visibility: WidgetVisibility,
    onChange: ((WidgetVisibility) -> WidgetVisibility) -> Unit
) {
    SettingsSectionCard(title = "Widget Visibility") {
        Text(
            "Tip: on the Home screen, hold and drag any widget to reorder it.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        ToggleRow("Deadlines", visibility.countdownEnabled) { checked -> onChange { it.copy(countdownEnabled = checked) } }
        ToggleRow("Study Timer / Pomodoro", visibility.pomodoroEnabled) { checked -> onChange { it.copy(pomodoroEnabled = checked) } }
        ToggleRow("Daily To-Do List", visibility.todoEnabled) { checked -> onChange { it.copy(todoEnabled = checked) } }
        ToggleRow("Backlog", visibility.chapterBacklogEnabled) { checked -> onChange { it.copy(chapterBacklogEnabled = checked) } }
        ToggleRow("Quick PDF Launcher", visibility.pdfLauncherEnabled) { checked -> onChange { it.copy(pdfLauncherEnabled = checked) } }
        ToggleRow("Focus Mode Toggle", visibility.focusModeEnabled) { checked -> onChange { it.copy(focusModeEnabled = checked) } }
        ToggleRow("Targets", visibility.milestoneEnabled) { checked -> onChange { it.copy(milestoneEnabled = checked) } }
        ToggleRow("App Shortcuts", visibility.appShortcutsEnabled) { checked -> onChange { it.copy(appShortcutsEnabled = checked) } }
        ToggleRow("Status (Wi-Fi / Battery / Bluetooth)", visibility.systemStatusEnabled) { checked -> onChange { it.copy(systemStatusEnabled = checked) } }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
