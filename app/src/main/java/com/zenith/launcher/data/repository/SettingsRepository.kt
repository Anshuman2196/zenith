package com.zenith.launcher.data.repository

import com.zenith.launcher.data.local.PreferencesManager
import com.zenith.launcher.data.model.BackgroundSettings
import com.zenith.launcher.data.model.ChapterItem
import com.zenith.launcher.data.model.ExamSettings
import com.zenith.launcher.data.model.FontChoice
import com.zenith.launcher.data.model.PdfLink
import com.zenith.launcher.data.model.TodoItem
import com.zenith.launcher.data.model.WidgetVisibility
import kotlinx.coroutines.flow.Flow

/**
 * Thin repository facade over [PreferencesManager] for everything except installed apps.
 * ViewModels depend on this instead of DataStore directly, keeping persistence swappable later.
 */
class SettingsRepository(private val prefs: PreferencesManager) {

    val profileName: Flow<String> = prefs.profileName
    suspend fun setProfileName(name: String) = prefs.setProfileName(name)

    val examSettings: Flow<ExamSettings> = prefs.examSettings
    suspend fun setJeeMainDate(epochMillis: Long?) = prefs.setJeeMainDate(epochMillis)
    suspend fun setJeeAdvancedDate(epochMillis: Long?) = prefs.setJeeAdvancedDate(epochMillis)

    val isDarkMode: Flow<Boolean> = prefs.isDarkMode
    suspend fun setDarkMode(enabled: Boolean) = prefs.setDarkMode(enabled)

    val fontChoice: Flow<FontChoice> = prefs.fontChoice
    suspend fun setFontChoice(choice: FontChoice) = prefs.setFontChoice(choice)

    val iconPackPackage: Flow<String?> = prefs.iconPackPackage
    suspend fun setIconPackPackage(pkg: String?) = prefs.setIconPackPackage(pkg)

    val widgetVisibility: Flow<WidgetVisibility> = prefs.widgetVisibility
    suspend fun setWidgetVisibility(visibility: WidgetVisibility) = prefs.setWidgetVisibility(visibility)

    val widgetColumns: Flow<List<List<String>>> = prefs.widgetColumns
    suspend fun setWidgetColumns(columns: List<List<String>>) = prefs.setWidgetColumns(columns)

    val todoList: Flow<List<TodoItem>> = prefs.todoList
    suspend fun setTodoList(items: List<TodoItem>) = prefs.setTodoList(items)

    val chapterList: Flow<List<ChapterItem>> = prefs.chapterList
    suspend fun setChapterList(items: List<ChapterItem>) = prefs.setChapterList(items)

    val pdfList: Flow<List<PdfLink>> = prefs.pdfList
    suspend fun setPdfList(items: List<PdfLink>) = prefs.setPdfList(items)

    val focusModeActive: Flow<Boolean> = prefs.focusModeActive
    suspend fun setFocusModeActive(active: Boolean) = prefs.setFocusModeActive(active)

    val focusAllowedApps: Flow<Set<String>> = prefs.focusAllowedApps
    suspend fun setFocusAllowedApps(packages: Set<String>) = prefs.setFocusAllowedApps(packages)

    val backgroundSettings: Flow<BackgroundSettings> = prefs.backgroundSettings
    suspend fun setBackgroundImageUri(uriString: String?) = prefs.setBackgroundImageUri(uriString)
    suspend fun setBackgroundColor(argb: Long?) = prefs.setBackgroundColor(argb)
    suspend fun clearBackground() = prefs.clearBackground()
}
