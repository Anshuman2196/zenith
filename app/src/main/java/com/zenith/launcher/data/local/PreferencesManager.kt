package com.zenith.launcher.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.zenith.launcher.data.model.BackgroundSettings
import com.zenith.launcher.data.model.ChapterItem
import com.zenith.launcher.data.model.ExamSettings
import com.zenith.launcher.data.model.PdfLink
import com.zenith.launcher.data.model.TodoItem
import com.zenith.launcher.data.model.WidgetIds
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
 * Lists (todos/chapters/pdf links/allowed apps/widget order) are stored as a single JSON string
 * per key, since DataStore Preferences only supports primitive types natively.
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
        val WIDGET_ORDER = stringPreferencesKey("widget_order_json")
        val TODO_LIST = stringPreferencesKey("todo_list_json")
        val CHAPTER_LIST = stringPreferencesKey("chapter_list_json")
        val PDF_LIST = stringPreferencesKey("pdf_list_json")
        val FOCUS_MODE_ACTIVE = booleanPreferencesKey("focus_mode_active")
        // Renamed from the old "blocked apps" key: Focus Mode is now an allow-list (pick the
        // apps you WANT visible while focused), so this deliberately doesn't reuse the old key.
        val FOCUS_ALLOWED_APPS = stringPreferencesKey("focus_allowed_apps_json")
        val BACKGROUND_IMAGE_URI = stringPreferencesKey("background_image_uri")
        val BACKGROUND_COLOR = longPreferencesKey("background_color_argb")
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

    // ---------- Widget order (drag-to-reorder) ----------

    /** Ids from [WidgetIds]. Any id missing from a saved (older) list is appended at the end. */
    val widgetOrder: Flow<List<String>> = context.dataStore.data.map { prefs ->
        val saved = prefs[Keys.WIDGET_ORDER]
            ?.let { runCatching { json.decodeFromString<List<String>>(it) }.getOrNull() }
            ?: WidgetIds.DEFAULT_ORDER
        val missing = WidgetIds.DEFAULT_ORDER.filterNot { it in saved }
        saved + missing
    }

    suspend fun setWidgetOrder(order: List<String>) {
        context.dataStore.edit { it[Keys.WIDGET_ORDER] = json.encodeToString(order) }
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

    /**
     * Package names the user has explicitly allowed - the ONLY apps shown while Focus Mode is
     * ON. Everything not in this set is hidden. Starts empty, so a first-time user picks in.
     */
    val focusAllowedApps: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[Keys.FOCUS_ALLOWED_APPS]
            ?.let { runCatching { json.decodeFromString<List<String>>(it) }.getOrNull() }
            ?.toSet() ?: emptySet()
    }

    suspend fun setFocusAllowedApps(packages: Set<String>) {
        context.dataStore.edit { it[Keys.FOCUS_ALLOWED_APPS] = json.encodeToString(packages.toList()) }
    }

    // ---------- Background customization ----------

    val backgroundSettings: Flow<BackgroundSettings> = context.dataStore.data.map { prefs ->
        BackgroundSettings(
            imageUri = prefs[Keys.BACKGROUND_IMAGE_URI],
            colorArgb = prefs[Keys.BACKGROUND_COLOR]
        )
    }

    /** Setting an image clears any solid color, and vice versa, so exactly one is active. */
    suspend fun setBackgroundImageUri(uriString: String?) {
        context.dataStore.edit {
            if (uriString == null) it.remove(Keys.BACKGROUND_IMAGE_URI) else {
                it[Keys.BACKGROUND_IMAGE_URI] = uriString
                it.remove(Keys.BACKGROUND_COLOR)
            }
        }
    }

    suspend fun setBackgroundColor(argb: Long?) {
        context.dataStore.edit {
            if (argb == null) it.remove(Keys.BACKGROUND_COLOR) else {
                it[Keys.BACKGROUND_COLOR] = argb
                it.remove(Keys.BACKGROUND_IMAGE_URI)
            }
        }
    }

    suspend fun clearBackground() {
        context.dataStore.edit {
            it.remove(Keys.BACKGROUND_IMAGE_URI)
            it.remove(Keys.BACKGROUND_COLOR)
        }
    }
}
