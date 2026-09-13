package com.zenith.launcher.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val installedApps: List<AppInfo> = emptyList(), // used by the Focus Mode app picker
    val focusAllowedApps: Set<String> = emptySet(),
    val background: BackgroundSettings = BackgroundSettings()
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

    init {
        viewModelScope.launch { _iconPacks.value = appRepository.getInstalledIconPacks() }
        viewModelScope.launch { _installedApps.value = appRepository.getInstalledApps() }
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.profileName,
        settingsRepository.examSettings,
        isDarkMode,
        fontChoice,
        _iconPacks,
        settingsRepository.iconPackPackage,
        settingsRepository.widgetVisibility,
        _installedApps,
        settingsRepository.focusAllowedApps,
        settingsRepository.backgroundSettings
    ) { array ->
        SettingsUiState(
            profileName = array[0] as String,
            examSettings = array[1] as ExamSettings,
            isDarkMode = array[2] as Boolean,
            fontChoice = array[3] as FontChoice,
            availableIconPacks = array[4] as List<IconPackInfo>,
            selectedIconPack = array[5] as String?,
            widgetVisibility = array[6] as WidgetVisibility,
            installedApps = array[7] as List<AppInfo>,
            focusAllowedApps = array[8] as Set<String>,
            background = array[9] as BackgroundSettings
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    // ---------- Profile ----------
    fun updateProfileName(name: String) = viewModelScope.launch { settingsRepository.setProfileName(name) }

    // ---------- Exam dates ----------
    fun updateJeeMainDate(epochMillis: Long?) = viewModelScope.launch { settingsRepository.setJeeMainDate(epochMillis) }
    fun updateJeeAdvancedDate(epochMillis: Long?) = viewModelScope.launch { settingsRepository.setJeeAdvancedDate(epochMillis) }

    // ---------- Theme ----------
    fun setDarkMode(enabled: Boolean) = viewModelScope.launch { settingsRepository.setDarkMode(enabled) }

    // ---------- Font ----------
    fun setFontChoice(choice: FontChoice) = viewModelScope.launch { settingsRepository.setFontChoice(choice) }

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
    }

    fun setBackgroundColor(argb: Long?) = viewModelScope.launch {
        settingsRepository.setBackgroundColor(argb)
    }

    fun resetBackground() = viewModelScope.launch { settingsRepository.clearBackground() }
}
