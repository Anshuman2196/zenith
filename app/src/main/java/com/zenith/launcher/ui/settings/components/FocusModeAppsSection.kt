package com.zenith.launcher.ui.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zenith.launcher.data.model.AppInfo

/**
 * Focus Mode App Picker: choose which apps are ALLOWED to stay visible while Focus Mode is on.
 * Everything left unchecked is hidden from the app drawer/grid the moment Focus Mode switches on
 * - an allow-list, so distraction-free by default rather than needing to hide every distraction
 * one by one.
 */
@Composable
fun FocusModeAppsSection(
    apps: List<AppInfo>,
    allowedPackages: Set<String>,
    onToggleApp: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filteredApps = remember(apps, query) {
        if (query.isBlank()) apps else apps.filter { it.label.contains(query, ignoreCase = true) }
    }
    SettingsSectionCard(title = "Focus Mode - Allowed Apps") {
        Text(
            "Pick the apps you want to stay usable while Focus Mode is on (e.g. calculator, notes). Everything else is hidden.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search apps") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
            items(filteredApps, key = { it.packageName + it.activityClassName }) { app ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(app.label, style = MaterialTheme.typography.bodyMedium)
                    Checkbox(
                        checked = app.packageName in allowedPackages,
                        onCheckedChange = { onToggleApp(app.packageName) }
                    )
                }
            }
        }
    }
}
