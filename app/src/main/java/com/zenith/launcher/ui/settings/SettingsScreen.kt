package com.zenith.launcher.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zenith.launcher.ui.settings.components.ExamSettingsSection
import com.zenith.launcher.ui.settings.components.FocusModeAppsSection
import com.zenith.launcher.ui.settings.components.IconPackPickerSection
import com.zenith.launcher.ui.settings.components.ProfileSettingsSection
import com.zenith.launcher.ui.settings.components.ThemeToggleSection
import com.zenith.launcher.ui.settings.components.WidgetVisibilitySection

/**
 * Dedicated Launcher Settings screen. Every control here writes straight through to DataStore
 * via [SettingsViewModel], so Home reflects changes immediately - no explicit "Save" button
 * except on the Profile Name field, which commits on tap.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Launcher Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                ProfileSettingsSection(currentName = state.profileName, onSave = viewModel::updateProfileName)
            }
            item {
                ExamSettingsSection(
                    examSettings = state.examSettings,
                    onJeeMainDateChange = viewModel::updateJeeMainDate,
                    onJeeAdvancedDateChange = viewModel::updateJeeAdvancedDate
                )
            }
            item {
                ThemeToggleSection(isDarkMode = state.isDarkMode, onToggle = viewModel::setDarkMode)
            }
            item {
                IconPackPickerSection(
                    availablePacks = state.availableIconPacks,
                    selectedPackage = state.selectedIconPack,
                    onSelect = viewModel::selectIconPack
                )
            }
            item {
                WidgetVisibilitySection(visibility = state.widgetVisibility, onChange = viewModel::setWidgetVisibility)
            }
            item {
                FocusModeAppsSection(
                    apps = state.installedApps,
                    blockedPackages = state.focusBlockedApps,
                    onToggleApp = viewModel::toggleFocusBlockedApp
                )
            }
        }
    }
}
