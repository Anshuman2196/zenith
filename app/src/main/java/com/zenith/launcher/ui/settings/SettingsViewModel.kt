package com.zenith.launcher.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenith.launcher.data.model.AppCategory
import com.zenith.launcher.data.model.AppInfo
import com.zenith.launcher.data.model.BackgroundSettings
import com.zenith.launcher.data.model.ExamSettings
import com.zenith.launcher.data.model.FontChoice
import com.zenith.launcher.data.model.IconPackInfo
import com.zenith.launcher.data.model.WidgetVisibility
import com.zenith.launcher.data.repository.AppRepository
import com.zenith.launcher.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Full settings snapshot backing every section of [SettingsScreen]. */
data class SettingsUiState(
    val profileName: String = "",
    val examSettings: ExamSettings = ExamSettings(),
    val isDarkMode: Boolean = true,
    val fontChoice: FontChoice = FontChoice.DEFAULT,
    val availableIconPacks: List<IconPackInfo> = emptyList(),
    val selectedIconPack: String? = null,
    val widgetVisibility: WidgetVisibility = WidgetVisibility(),
    val installedApps: List<AppInfo> = emptyList(), // used by the Focus Mode app picker + App Categories
    val focusAllowedApps: Set<String> = emptySet(),
    val background: BackgroundSettings = BackgroundSettings(),
    val syncLockScreenWallpaper: Boolean = true,
    val lockOnDoubleTap: Boolean = false,
    val appCategories: Map<String, String> = emptyMap(),
    val appCategoryTypes: List<String> = emptyList()
)

class SettingsViewModel(
    private val appRepository: AppRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _iconPacks = MutableStateFlow<List<IconPackInfo>>(emptyList())
    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())

    // Exposed separately so MainActivity can theme the app without collecting the whole
    // (heavier) uiState combine chain just to read one boolean.
    val isDarkMode: StateFlow<Boolean> =
        settingsRepository.isDarkMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // Exposed separately for the same reason as isDarkMode: MainActivity needs this to build the
    // theme before the rest of the app renders, without collecting the whole uiState chain.
    val fontChoice: StateFlow<FontChoice> =
        settingsRepository.fontChoice.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FontChoice.DEFAULT)

    val customFontPath: StateFlow<String?> =
        settingsRepository.customFontPath.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        viewModelScope.launch { _iconPacks.value = appRepository.getInstalledIconPacks() }
        viewModelScope.launch { _installedApps.value = appRepository.getInstalledApps() }
    }

    val uiState: StateFlow<SettingsUiState> = settingsRepository.profileName.combine(settingsRepository.examSettings) { profileName, exams ->
        SettingsUiState(profileName = profileName, examSettings = exams)
    }.combine(isDarkMode) { state, darkMode -> state.copy(isDarkMode = darkMode) }
        .combine(fontChoice) { state, font -> state.copy(fontChoice = font) }
        .combine(_iconPacks) { state, packs -> state.copy(availableIconPacks = packs) }
        .combine(settingsRepository.iconPackPackage) { state, pack -> state.copy(selectedIconPack = pack) }
        .combine(settingsRepository.widgetVisibility) { state, visibility -> state.copy(widgetVisibility = visibility) }
        .combine(_installedApps) { state, apps -> state.copy(installedApps = apps) }
        .combine(settingsRepository.focusAllowedApps) { state, allowed -> state.copy(focusAllowedApps = allowed) }
        .combine(settingsRepository.backgroundSettings) { state, background -> state.copy(background = background) }
        .combine(settingsRepository.syncLockScreenWallpaper) { state, sync -> state.copy(syncLockScreenWallpaper = sync) }
        .combine(settingsRepository.lockOnDoubleTap) { state, enabled -> state.copy(lockOnDoubleTap = enabled) }
        .combine(settingsRepository.appCategories) { state, categories -> state.copy(appCategories = categories) }
        .combine(settingsRepository.appCategoryTypes) { state, types -> state.copy(appCategoryTypes = types) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    // ---------- Profile ----------
    fun updateProfileName(name: String) = viewModelScope.launch { settingsRepository.setProfileName(name) }

    // ---------- Exam dates ----------
    fun setExamSettings(settings: ExamSettings) = viewModelScope.launch { settingsRepository.setExamSettings(settings) }

    // ---------- Theme ----------
    fun setDarkMode(enabled: Boolean) = viewModelScope.launch { settingsRepository.setDarkMode(enabled) }

    // ---------- Font ----------
    fun setFontChoice(choice: FontChoice) = viewModelScope.launch { settingsRepository.setFontChoice(choice) }
    fun setCustomFontPath(path: String?) = viewModelScope.launch { settingsRepository.setCustomFontPath(path) }

    // ---------- Icon pack ----------
    fun selectIconPack(packageName: String?) = viewModelScope.launch { settingsRepository.setIconPackPackage(packageName) }

    // ---------- Widget visibility ----------
    fun setWidgetVisibility(update: (WidgetVisibility) -> WidgetVisibility) = viewModelScope.launch {
        settingsRepository.setWidgetVisibility(update(uiState.value.widgetVisibility))
    }

    // ---------- Focus mode allow-list ----------
    fun toggleFocusAllowedApp(packageName: String) = viewModelScope.launch {
        val current = uiState.value.focusAllowedApps
        val updated = if (packageName in current) current - packageName else current + packageName
        settingsRepository.setFocusAllowedApps(updated)
    }

    // ---------- Background customization ----------
    fun setBackgroundImage(uriString: String?) = viewModelScope.launch {
        settingsRepository.setBackgroundImageUri(uriString)
        if (uriString != null) {
            appRepository.syncWallpaper(uriString, alsoLockScreen = uiState.value.syncLockScreenWallpaper)
        }
    }

    fun setBackgroundColor(argb: Long?) = viewModelScope.launch {
        settingsRepository.setBackgroundColor(argb)
    }

    fun resetBackground() = viewModelScope.launch { settingsRepository.clearBackground() }

    fun setSyncLockScreenWallpaper(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setSyncLockScreenWallpaper(enabled)
        // Re-apply immediately so toggling this on retroactively covers whatever photo is
        // already selected, instead of only taking effect the next time one is picked.
        uiState.value.background.imageUri?.let { appRepository.syncWallpaper(it, alsoLockScreen = enabled) }
    }

    // ---------- Double-tap to lock ----------
    fun setLockOnDoubleTap(enabled: Boolean) = viewModelScope.launch { settingsRepository.setLockOnDoubleTap(enabled) }

    // ---------- App Drawer categories ----------
    fun setAppCategory(packageName: String, category: String) = viewModelScope.launch { settingsRepository.setAppCategory(packageName, category) }
    fun addAppCategoryType(label: String) = viewModelScope.launch { settingsRepository.addAppCategoryType(label) }
}
