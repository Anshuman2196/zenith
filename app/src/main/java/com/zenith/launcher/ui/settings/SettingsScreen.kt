package com.zenith.launcher.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SwipeRight
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.zenith.launcher.ui.settings.components.AppsControlCenterSection
import com.zenith.launcher.ui.settings.components.AttentionProtectionSection
import com.zenith.launcher.ui.settings.components.BackgroundSettingsSection
import com.zenith.launcher.ui.settings.components.DeadlineSettingsSection
import com.zenith.launcher.ui.settings.components.FontSettingsSection
import com.zenith.launcher.ui.settings.components.GesturesSettingsSection
import com.zenith.launcher.ui.settings.components.IconPackPickerSection
import com.zenith.launcher.ui.settings.components.ProfileSettingsSection
import com.zenith.launcher.ui.settings.components.ThemeToggleSection
import com.zenith.launcher.ui.settings.components.WidgetVisibilitySection

/** Top-level groupings settings are organized under - see [SettingsScreen]'s doc for why. */
private enum class SettingsCategory(val title: String, val subtitle: String, val icon: ImageVector) {
    PROFILE_DEADLINES("Profile & Deadlines", "Your name and the deadlines you're tracking", Icons.Default.Person),
    APPEARANCE("Appearance", "Theme, font, icon pack, wallpaper", Icons.Default.Palette),
    WIDGETS("Widgets", "Choose what shows up on Home", Icons.Default.Widgets),
    GESTURES("Gestures & System", "Protect attention and control system surfaces", Icons.Default.SwipeRight),
    APPS("Apps", "Focus Mode, Distractions, and App Drawer categories", Icons.Default.Apps)
}

/**
 * Dedicated Zenith Settings screen. Every control here writes straight through to DataStore
 * via [SettingsViewModel], so Home reflects changes immediately - no explicit "Save" button
 * except on the Profile Name field, which commits on tap.
 *
 * With as many sections as this app now has, one long scrolling list got unwieldy, so this is
 * organized the same way Android's own Settings app is: a menu of categories up front, each
 * opening its own focused sub-screen (see [SettingsCategory]).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    var selectedCategory by remember { mutableStateOf<SettingsCategory?>(null) }

    // System back steps up one level (sub-screen -> category menu -> Home) rather than always
    // exiting Settings outright.
    BackHandler(enabled = selectedCategory != null) { selectedCategory = null }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(selectedCategory?.title ?: "Zenith Settings") },
                navigationIcon = {
                    IconButton(onClick = { if (selectedCategory != null) selectedCategory = null else onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        when (selectedCategory) {
            null -> SettingsCategoryMenu(modifier = Modifier.padding(padding), onSelect = { selectedCategory = it })
            SettingsCategory.PROFILE_DEADLINES -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item { ProfileSettingsSection(currentName = state.profileName, onSave = viewModel::updateProfileName) }
                item { DeadlineSettingsSection(deadlineSettings = state.deadlineSettings, onChange = viewModel::setDeadlineSettings) }
            }
            SettingsCategory.APPEARANCE -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item { ThemeToggleSection(isDarkMode = state.isDarkMode, onToggle = viewModel::setDarkMode) }
                item { FontSettingsSection(selected = state.fontChoice, customFontPath = viewModel.customFontPath.collectAsState().value, onSelect = viewModel::setFontChoice, onImportFont = viewModel::setCustomFontPath) }
                item {
                    IconPackPickerSection(
                        availablePacks = state.availableIconPacks,
                        selectedPackage = state.selectedIconPack,
                        onSelect = viewModel::selectIconPack
                    )
                }
                item {
                    BackgroundSettingsSection(
                        background = state.background,
                        syncLockScreenWallpaper = state.syncLockScreenWallpaper,
                        onPickImage = viewModel::setBackgroundImage,
                        onPickColor = viewModel::setBackgroundColor,
                        onReset = viewModel::resetBackground,
                        onSyncLockScreenWallpaperChange = viewModel::setSyncLockScreenWallpaper
                    )
                }
            }
            SettingsCategory.WIDGETS -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item { WidgetVisibilitySection(visibility = state.widgetVisibility, onChange = viewModel::setWidgetVisibility) }
            }
            SettingsCategory.GESTURES -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item {
                    GesturesSettingsSection(
                        lockOnDoubleTap = state.lockOnDoubleTap,
                        onLockOnDoubleTapChange = viewModel::setLockOnDoubleTap
                    )
                }
                item {
                    AttentionProtectionSection(
                        selected = state.attentionProtectionMode,
                        onSelect = viewModel::setAttentionProtectionMode
                    )
                }
            }
            SettingsCategory.APPS -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item {
                    AppsControlCenterSection(
                        apps = state.installedApps,
                        allowedPackages = state.focusAllowedApps,
                        distractionPackages = state.distractionApps,
                        categories = state.appCategories,
                        categoryTypes = state.appCategoryTypes,
                        onToggleAllowed = viewModel::toggleFocusAllowedApp,
                        onToggleDistraction = viewModel::toggleDistractionApp,
                        onSetCategory = viewModel::setAppCategory,
                        onAddCategory = viewModel::addAppCategoryType
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsCategoryMenu(modifier: Modifier = Modifier, onSelect: (SettingsCategory) -> Unit) {
    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 8.dp)) {
        items(SettingsCategory.entries) { category ->
            ListItem(
                headlineContent = { Text(category.title) },
                supportingContent = { Text(category.subtitle, style = MaterialTheme.typography.labelSmall) },
                leadingContent = { Icon(category.icon, contentDescription = null) },
                trailingContent = {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(category) }
                    .padding(horizontal = 8.dp)
            )
        }
    }
}
