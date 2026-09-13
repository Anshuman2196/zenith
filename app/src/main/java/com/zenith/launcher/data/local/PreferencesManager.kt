package com.zenith.launcher.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.zenith.launcher.data.model.ChapterItem
import com.zenith.launcher.data.model.ExamSettings
import com.zenith.launcher.data.model.PdfLink
import com.zenith.launcher.data.model.TodoItem
import com.zenith.launcher.data.model.WidgetVisibility
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Single top-level DataStore instance for the whole app process. */
private val Context.dataStore by preferencesDataStore(name = "launcher_settings")

/**
 * Central read/write point for every persisted launcher setting. Each setting is exposed as a
 * Flow so ViewModels collect it reactively and the UI recomposes automatically on change.
 *
 * Lists (todos/chapters/pdf links/blocked apps) are stored as a single JSON string per key,
 * since DataStore Preferences only supports primitive types natively.
 */
class PreferencesManager(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    private object Keys {
        val PROFILE_NAME = stringPreferencesKey("profile_name")
        val JEE_MAIN_DATE = longPreferencesKey("jee_main_date")
        val JEE_ADVANCED_DATE = longPreferencesKey("jee_advanced_date")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val ICON_PACK_PACKAGE = stringPreferencesKey("icon_pack_package")
        val WIDGET_VISIBILITY = stringPreferencesKey("widget_visibility_json")
        val TODO_LIST = stringPreferencesKey("todo_list_json")
        val CHAPTER_LIST = stringPreferencesKey("chapter_list_json")
        val PDF_LIST = stringPreferencesKey("pdf_list_json")
        val FOCUS_MODE_ACTIVE = booleanPreferencesKey("focus_mode_active")
        val FOCUS_BLOCKED_APPS = stringPreferencesKey("focus_blocked_apps_json")
    }

    // ---------- Profile ----------

    val profileName: Flow<String> = context.dataStore.data.map { it[Keys.PROFILE_NAME] ?: "Aspirant" }

    suspend fun setProfileName(name: String) {
        context.dataStore.edit { it[Keys.PROFILE_NAME] = name }
    }

    // ---------- Exam dates ----------

    val examSettings: Flow<ExamSettings> = context.dataStore.data.map {
        ExamSettings(
            jeeMainDateMillis = it[Keys.JEE_MAIN_DATE],
            jeeAdvancedDateMillis = it[Keys.JEE_ADVANCED_DATE]
        )
    }

    suspend fun setJeeMainDate(epochMillis: Long?) {
        context.dataStore.edit {
            if (epochMillis == null) it.remove(Keys.JEE_MAIN_DATE) else it[Keys.JEE_MAIN_DATE] = epochMillis
        }
    }

    suspend fun setJeeAdvancedDate(epochMillis: Long?) {
        context.dataStore.edit {
            if (epochMillis == null) it.remove(Keys.JEE_ADVANCED_DATE) else it[Keys.JEE_ADVANCED_DATE] = epochMillis
        }
    }

    // ---------- Theme ----------

    // Default true: dark mode is eye-friendlier for late-night study sessions.
    val isDarkMode: Flow<Boolean> = context.dataStore.data.map { it[Keys.DARK_MODE] ?: true }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DARK_MODE] = enabled }
    }

    // ---------- Icon pack ----------

    val iconPackPackage: Flow<String?> = context.dataStore.data.map { it[Keys.ICON_PACK_PACKAGE] }

    suspend fun setIconPackPackage(packageName: String?) {
        context.dataStore.edit {
            if (packageName == null) it.remove(Keys.ICON_PACK_PACKAGE) else it[Keys.ICON_PACK_PACKAGE] = packageName
        }
    }

    // ---------- Widget visibility ----------

    val widgetVisibility: Flow<WidgetVisibility> = context.dataStore.data.map { prefs ->
        prefs[Keys.WIDGET_VISIBILITY]
            ?.let { runCatching { json.decodeFromString<WidgetVisibility>(it) }.getOrNull() }
            ?: WidgetVisibility()
    }

    suspend fun setWidgetVisibility(visibility: WidgetVisibility) {
        context.dataStore.edit { it[Keys.WIDGET_VISIBILITY] = json.encodeToString(visibility) }
    }

    // ---------- Daily To-Do list ----------

    val todoList: Flow<List<TodoItem>> = context.dataStore.data.map { prefs ->
        prefs[Keys.TODO_LIST]?.let { runCatching { json.decodeFromString<List<TodoItem>>(it) }.getOrNull() } ?: emptyList()
    }

    suspend fun setTodoList(items: List<TodoItem>) {
        context.dataStore.edit { it[Keys.TODO_LIST] = json.encodeToString(items) }
    }

    // ---------- Chapter backlog ----------

    val chapterList: Flow<List<ChapterItem>> = context.dataStore.data.map { prefs ->
        prefs[Keys.CHAPTER_LIST]?.let { runCatching { json.decodeFromString<List<ChapterItem>>(it) }.getOrNull() } ?: emptyList()
    }

    suspend fun setChapterList(items: List<ChapterItem>) {
        context.dataStore.edit { it[Keys.CHAPTER_LIST] = json.encodeToString(items) }
    }

    // ---------- PDF quick-launch links ----------

    val pdfList: Flow<List<PdfLink>> = context.dataStore.data.map { prefs ->
        prefs[Keys.PDF_LIST]?.let { runCatching { json.decodeFromString<List<PdfLink>>(it) }.getOrNull() } ?: emptyList()
    }

    suspend fun setPdfList(items: List<PdfLink>) {
        context.dataStore.edit { it[Keys.PDF_LIST] = json.encodeToString(items) }
    }

    // ---------- Focus mode ----------

    val focusModeActive: Flow<Boolean> = context.dataStore.data.map { it[Keys.FOCUS_MODE_ACTIVE] ?: false }

    suspend fun setFocusModeActive(active: Boolean) {
        context.dataStore.edit { it[Keys.FOCUS_MODE_ACTIVE] = active }
    }

    /** Package names the user has flagged as "distracting" - hidden whenever Focus Mode is ON. */
    val focusBlockedApps: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[Keys.FOCUS_BLOCKED_APPS]
            ?.let { runCatching { json.decodeFromString<List<String>>(it) }.getOrNull() }
            ?.toSet() ?: emptySet()
    }

    suspend fun setFocusBlockedApps(packages: Set<String>) {
        context.dataStore.edit { it[Keys.FOCUS_BLOCKED_APPS] = json.encodeToString(packages.toList()) }
    }
}
