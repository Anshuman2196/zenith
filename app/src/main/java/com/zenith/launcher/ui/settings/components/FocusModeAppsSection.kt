package com.zenith.launcher.ui.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zenith.launcher.data.model.AppInfo

/**
 * Focus Mode App Picker: choose which apps count as "distracting" and get hidden from the
 * Home screen grid whenever Focus Mode is switched on.
 */
@Composable
fun FocusModeAppsSection(
    apps: List<AppInfo>,
    blockedPackages: Set<String>,
    onToggleApp: (String) -> Unit
) {
    SettingsSectionCard(title = "Focus Mode - Blocked Apps") {
        Text(
            "Pick the apps you want hidden while Focus Mode is on (e.g. social media, games).",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
            items(apps, key = { it.packageName + it.activityClassName }) { app ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(app.label, style = MaterialTheme.typography.bodyMedium)
                    Checkbox(
                        checked = app.packageName in blockedPackages,
                        onCheckedChange = { onToggleApp(app.packageName) }
                    )
                }
            }
        }
    }
}
