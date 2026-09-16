package com.zenith.launcher.ui.settings.components

import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.zenith.launcher.data.model.AppInfo

/** A focused Apps control centre that separates Focus Mode permissions from drawer organisation. */
@Composable
fun AppsControlCenterSection(
    apps: List<AppInfo>,
    allowedPackages: Set<String>,
    distractionPackages: Set<String>,
    categories: Map<String, String>,
    categoryTypes: List<String>,
    onToggleAllowed: (String) -> Unit,
    onToggleDistraction: (String) -> Unit,
    onSetCategory: (String, String) -> Unit,
    onAddCategory: (String) -> Unit
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    SettingsSectionCard(title = "Apps control centre") {
        Text("Choose what stays available in Focus Mode, what gets a five-second pause, and how apps are organized.")
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Focus Mode") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Pause") })
            Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("Drawer") })
        }
        if (tab == 0) {
            FocusModeAppsSection(apps, allowedPackages, onToggleAllowed, showCard = false)
        } else if (tab == 1) {
            DistractionAppsSection(apps, distractionPackages, onToggleDistraction, showCard = false)
        } else {
            AppCategorySettingsSection(apps, categories, categoryTypes, onSetCategory, onAddCategory, showCard = false)
        }
    }
}
