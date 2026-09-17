package com.zenith.launcher.ui.settings
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SwipeRight
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.zenith.launcher.ui.settings.components.AppsControlCenterSection
import com.zenith.launcher.ui.settings.components.AttentionProtectionSection
import com.zenith.launcher.ui.settings.components.BackgroundSettingsSection
import com.zenith.launcher.ui.settings.components.FontSettingsSection
import com.zenith.launcher.ui.settings.components.GesturesSettingsSection
import com.zenith.launcher.ui.settings.components.IconPackPickerSection
import com.zenith.launcher.ui.settings.components.ProfileSettingsSection
import com.zenith.launcher.ui.settings.components.ThemeToggleSection
import com.zenith.launcher.ui.settings.components.WidgetVisibilitySection
import com.zenith.launcher.ui.settings.components.UpdatesSettingsSection


private enum class SettingsCategory(val title: String, val subtitle: String, val icon: ImageVector) {
    PROFILE("Profile", "Name and personal details", Icons.Default.Person),
    APPEARANCE("Appearance", "Theme, type, icons and wallpaper", Icons.Default.Palette),
    WIDGETS("Home layout", "Choose and arrange what you see", Icons.Default.Widgets),
    GESTURES("Gestures & attention", "Navigation and attention protection", Icons.Default.SwipeRight),
    APPS("Apps", "Focus, distractions and categories", Icons.Default.Apps),
    UPDATES("Updates", "Keep Zenith current", Icons.Default.SystemUpdate)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit, onPreviewOnboarding: () -> Unit = {}) {
    val state by viewModel.uiState.collectAsState()
    var selectedCategory by remember { mutableStateOf<SettingsCategory?>(null) }
    val customFontPath by viewModel.customFontPath.collectAsState()

    BackHandler(enabled = selectedCategory != null) { selectedCategory = null }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(selectedCategory?.title ?: "Settings") },
                navigationIcon = {
                    IconButton(onClick = { if (selectedCategory != null) selectedCategory = null else onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (selectedCategory == null) {
            SettingsHome(
                modifier = Modifier.padding(padding),
                profileName = state.profileName,
                onSelect = { selectedCategory = it },
                onPreviewOnboarding = onPreviewOnboarding
            )
        } else {
            SettingsDetail(
                category = selectedCategory!!,
                state = state,
                customFontPath = customFontPath,
                viewModel = viewModel,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun SettingsHome(
    modifier: Modifier = Modifier,
    profileName: String,
    onSelect: (SettingsCategory) -> Unit,
    onPreviewOnboarding: () -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Make Zenith yours", style = MaterialTheme.typography.titleLarge)
                        Text(
                            if (profileName.isBlank()) "Shape your home around how you study." else "Welcome back, $profileName. Shape your home around how you study.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
        item {
            Text("Customize", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Onboarding preview", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Temporarily walk through the introduction again without resetting your onboarding status.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(onClick = onPreviewOnboarding) { Text("Check") }
                }
            }
        }
        items(SettingsCategory.entries) { category -> SettingsCategoryCard(category, onClick = { onSelect(category) }) }
        item {
            Text(
                "Deadlines, Todo and Backlog are managed directly from their Home widgets.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun SettingsCategoryCard(category: SettingsCategory, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Icon(category.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(category.title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                Text(category.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingsDetail(
    category: SettingsCategory,
    state: SettingsUiState,
    customFontPath: String?,
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(category.subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
        }
        when (category) {
            SettingsCategory.PROFILE -> item { ProfileSettingsSection(currentName = state.profileName, onSave = viewModel::updateProfileName) }
            SettingsCategory.APPEARANCE -> {
                item { ThemeToggleSection(isDarkMode = state.isDarkMode, onToggle = viewModel::setDarkMode) }
                item { FontSettingsSection(selected = state.fontChoice, customFontPath = customFontPath, onSelect = viewModel::setFontChoice, onImportFont = viewModel::setCustomFontPath) }
                item { IconPackPickerSection(availablePacks = state.availableIconPacks, selectedPackage = state.selectedIconPack, onSelect = viewModel::selectIconPack) }
                item { BackgroundSettingsSection(background = state.background, syncLockScreenWallpaper = state.syncLockScreenWallpaper, onPickImage = viewModel::setBackgroundImage, onPickColor = viewModel::setBackgroundColor, onReset = viewModel::resetBackground, onSyncLockScreenWallpaperChange = viewModel::setSyncLockScreenWallpaper) }
            }
            SettingsCategory.WIDGETS -> item { WidgetVisibilitySection(visibility = state.widgetVisibility, onChange = viewModel::setWidgetVisibility) }
            SettingsCategory.GESTURES -> {
                item { GesturesSettingsSection(lockOnDoubleTap = state.lockOnDoubleTap, onLockOnDoubleTapChange = viewModel::setLockOnDoubleTap) }
                item { AttentionProtectionSection(selected = state.attentionProtectionMode, onSelect = viewModel::setAttentionProtectionMode) }
            }
            SettingsCategory.APPS -> item { AppsControlCenterSection(apps = state.installedApps, allowedPackages = state.focusAllowedApps, distractionPackages = state.distractionApps, categories = state.appCategories, categoryTypes = state.appCategoryTypes, onToggleAllowed = viewModel::toggleFocusAllowedApp, onToggleDistraction = viewModel::toggleDistractionApp, onSetCategory = viewModel::setAppCategory, onAddCategory = viewModel::addAppCategoryType) }
            SettingsCategory.UPDATES -> item { UpdatesSettingsSection() }
        }
    }
}
