package com.zenith.launcher.data.repository

import com.zenith.launcher.data.local.PreferencesManager
import com.zenith.launcher.data.model.AttentionProtectionMode
import com.zenith.launcher.data.model.AppShortcutRef
import com.zenith.launcher.data.model.BackgroundSettings
import com.zenith.launcher.data.model.BacklogItem
import com.zenith.launcher.data.model.DeadlineSettings
import com.zenith.launcher.data.model.FontChoice
import com.zenith.launcher.data.model.StudyTarget
import com.zenith.launcher.data.model.LibraryLink
import com.zenith.launcher.data.model.TodoItem
import com.zenith.launcher.data.model.WidgetVisibility
import com.zenith.launcher.data.model.WidgetSize
import kotlinx.coroutines.flow.Flow

/**
 * Thin repository facade over [PreferencesManager] for everything except installed apps.
 * ViewModels depend on this instead of DataStore directly, keeping persistence swappable later.
 */
class SettingsRepository(private val prefs: PreferencesManager) {

    val onboardingCompleted: Flow<Boolean> = prefs.onboardingCompleted
    suspend fun setOnboardingCompleted(completed: Boolean) = prefs.setOnboardingCompleted(completed)

    val profileName: Flow<String> = prefs.profileName
    suspend fun setProfileName(name: String) = prefs.setProfileName(name)

    val deadlineSettings: Flow<DeadlineSettings> = prefs.deadlineSettings
    suspend fun setDeadlineSettings(settings: DeadlineSettings) = prefs.setDeadlineSettings(settings)

    val isDarkMode: Flow<Boolean> = prefs.isDarkMode
    suspend fun setDarkMode(enabled: Boolean) = prefs.setDarkMode(enabled)

    val fontChoice: Flow<FontChoice> = prefs.fontChoice
    suspend fun setFontChoice(choice: FontChoice) = prefs.setFontChoice(choice)

    val customFontPath: Flow<String?> = prefs.customFontPath
    suspend fun setCustomFontPath(path: String?) = prefs.setCustomFontPath(path)

    val iconPackPackage: Flow<String?> = prefs.iconPackPackage
    suspend fun setIconPackPackage(pkg: String?) = prefs.setIconPackPackage(pkg)

    val widgetVisibility: Flow<WidgetVisibility> = prefs.widgetVisibility
    suspend fun setWidgetVisibility(visibility: WidgetVisibility) = prefs.setWidgetVisibility(visibility)

    val widgetColumns: Flow<List<List<String>>> = prefs.widgetColumns
    suspend fun setWidgetColumns(columns: List<List<String>>) = prefs.setWidgetColumns(columns)
    val widgetSizes: Flow<Map<String, WidgetSize>> = prefs.widgetSizes
    suspend fun setWidgetSize(id: String, size: WidgetSize) = prefs.setWidgetSize(id, size)
    val widgetHeights: Flow<Map<String, Int>> = prefs.widgetHeights
    suspend fun setWidgetHeight(id: String, heightDp: Int) = prefs.setWidgetHeight(id, heightDp)

    val todoList: Flow<List<TodoItem>> = prefs.todoList
    suspend fun setTodoList(items: List<TodoItem>) = prefs.setTodoList(items)

    val backlogList: Flow<List<BacklogItem>> = prefs.backlogList
    suspend fun setBacklogList(items: List<BacklogItem>) = prefs.setBacklogList(items)

    val libraryList: Flow<List<LibraryLink>> = prefs.libraryList
    suspend fun setLibraryList(items: List<LibraryLink>) = prefs.setLibraryList(items)

    val focusModeActive: Flow<Boolean> = prefs.focusModeActive
    suspend fun setFocusModeActive(active: Boolean) = prefs.setFocusModeActive(active)

    val focusAllowedApps: Flow<Set<String>> = prefs.focusAllowedApps
    suspend fun setFocusAllowedApps(packages: Set<String>) = prefs.setFocusAllowedApps(packages)

    val distractionApps: Flow<Set<String>> = prefs.distractionApps
    suspend fun setDistractionApps(packages: Set<String>) = prefs.setDistractionApps(packages)

    val backgroundSettings: Flow<BackgroundSettings> = prefs.backgroundSettings
    suspend fun setBackgroundImageUri(uriString: String?) = prefs.setBackgroundImageUri(uriString)
    suspend fun setBackgroundColor(argb: Long?) = prefs.setBackgroundColor(argb)
    suspend fun clearBackground() = prefs.clearBackground()

    val syncLockScreenWallpaper: Flow<Boolean> = prefs.syncLockScreenWallpaper
    suspend fun setSyncLockScreenWallpaper(enabled: Boolean) = prefs.setSyncLockScreenWallpaper(enabled)


    val studyTargets: Flow<List<StudyTarget>> = prefs.studyTargets
    suspend fun setStudyTargets(targets: List<StudyTarget>) = prefs.setStudyTargets(targets)

    val appShortcuts: Flow<List<AppShortcutRef>> = prefs.appShortcuts
    suspend fun setAppShortcuts(shortcuts: List<AppShortcutRef>) = prefs.setAppShortcuts(shortcuts)

    val appCategories: Flow<Map<String, String>> = prefs.appCategories
    suspend fun setAppCategory(packageName: String, category: String) = prefs.setAppCategory(packageName, category)
    suspend fun assignDefaultCategories(defaults: Map<String, String>) = prefs.assignDefaultCategories(defaults)
    val appCategoryTypes: Flow<List<String>> = prefs.appCategoryTypes
    suspend fun addAppCategoryType(label: String) = prefs.addAppCategoryType(label)

    val lockOnDoubleTap: Flow<Boolean> = prefs.lockOnDoubleTap
    suspend fun setLockOnDoubleTap(enabled: Boolean) = prefs.setLockOnDoubleTap(enabled)

    val attentionProtectionMode: Flow<AttentionProtectionMode> = prefs.attentionProtectionMode
    suspend fun setAttentionProtectionMode(mode: AttentionProtectionMode) = prefs.setAttentionProtectionMode(mode)
}
