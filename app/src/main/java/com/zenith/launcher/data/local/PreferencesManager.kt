package com.zenith.launcher.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.zenith.launcher.data.model.AppCategory
import com.zenith.launcher.data.model.AppShortcutRef
import com.zenith.launcher.data.model.BackgroundSettings
import com.zenith.launcher.data.model.ChapterItem
import com.zenith.launcher.data.model.ExamSettings
import com.zenith.launcher.data.model.FontChoice
import com.zenith.launcher.data.model.MilestoneTarget
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

/** How many entries the Recent Apps deck (see RecentAppsOverlay) keeps. */
private const val MAX_RECENT_APPS = 15

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
        val EXAM_SETTINGS = stringPreferencesKey("exam_settings_json")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val FONT_CHOICE = stringPreferencesKey("font_choice")
        val CUSTOM_FONT_PATH = stringPreferencesKey("custom_font_path")
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
        val SYNC_LOCK_SCREEN_WALLPAPER = booleanPreferencesKey("sync_lock_screen_wallpaper")
        val MILESTONE_TARGET = stringPreferencesKey("milestone_target_json")
        val APP_SHORTCUTS = stringPreferencesKey("app_shortcuts_json")
        val RECENT_APPS = stringPreferencesKey("recent_apps_json")
        val APP_CATEGORIES = stringPreferencesKey("app_categories_json")
        val LOCK_ON_DOUBLE_TAP = booleanPreferencesKey("lock_on_double_tap")
    }

    // ---------- Profile ----------

    val profileName: Flow<String> = context.dataStore.data.map { it[Keys.PROFILE_NAME] ?: "Aspirant" }

    suspend fun setProfileName(name: String) {
        context.dataStore.edit { it[Keys.PROFILE_NAME] = name }
    }

    // ---------- Exams (fully user-managed list - see ExamTarget) ----------

    val examSettings: Flow<ExamSettings> = context.dataStore.data.map { prefs ->
        prefs[Keys.EXAM_SETTINGS]?.let { runCatching { json.decodeFromString<ExamSettings>(it) }.getOrNull() } ?: ExamSettings()
    }

    suspend fun setExamSettings(settings: ExamSettings) {
        context.dataStore.edit { it[Keys.EXAM_SETTINGS] = json.encodeToString(settings) }
    }

    // ---------- Theme ----------

    // Default true: dark mode is eye-friendlier for late-night study sessions.
    val isDarkMode: Flow<Boolean> = context.dataStore.data.map { it[Keys.DARK_MODE] ?: true }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DARK_MODE] = enabled }
    }

    // ---------- Font ----------

    val fontChoice: Flow<FontChoice> =
        context.dataStore.data.map { FontChoice.fromStorageValue(it[Keys.FONT_CHOICE]) }

    suspend fun setFontChoice(choice: FontChoice) {
        context.dataStore.edit { it[Keys.FONT_CHOICE] = choice.name }
    }

    /** Absolute path (in app-internal storage - see FontImportHelper) to a user-imported font file. */
    val customFontPath: Flow<String?> = context.dataStore.data.map { it[Keys.CUSTOM_FONT_PATH] }

    suspend fun setCustomFontPath(path: String?) {
        context.dataStore.edit { if (path == null) it.remove(Keys.CUSTOM_FONT_PATH) else it[Keys.CUSTOM_FONT_PATH] = path }
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

    // ---------- Widget columns (3-column drag-to-reorder grid) ----------

    /**
     * The Home screen's 3-column layout, each column an ordered list of widget ids from
     * [WidgetIds]. Understands two on-disk shapes under the same key for a seamless upgrade:
     * the current `List<List<String>>` (columns), and the older flat `List<String>` this app
     * used before the grid layout existed (round-robin'd into 3 columns so nobody's saved
     * arrangement is lost). Any widget id missing from a saved arrangement - e.g. a new widget
     * added in a later app version - is appended to the shortest column.
     */
    val widgetColumns: Flow<List<List<String>>> = context.dataStore.data.map { prefs ->
        val raw = prefs[Keys.WIDGET_ORDER]
        val columns: List<List<String>> = raw
            ?.let { runCatching { json.decodeFromString<List<List<String>>>(it) }.getOrNull() }
            ?: raw
                ?.let { runCatching { json.decodeFromString<List<String>>(it) }.getOrNull() }
                ?.let { flat -> roundRobinColumns(flat) }
            ?: WidgetIds.DEFAULT_COLUMNS

        val placed = columns.flatten().toSet()
        val missing = WidgetIds.DEFAULT_ORDER.filterNot { it in placed }
        if (missing.isEmpty()) columns else appendToShortestColumns(columns, missing)
    }

    suspend fun setWidgetColumns(columns: List<List<String>>) {
        context.dataStore.edit { it[Keys.WIDGET_ORDER] = json.encodeToString(columns) }
    }

    /** Distributes a flat widget order into 3 columns, i, i+3, i+6... in column i%3. */
    private fun roundRobinColumns(flat: List<String>): List<List<String>> {
        val columns = List(3) { mutableListOf<String>() }
        flat.forEachIndexed { index, id -> columns[index % 3].add(id) }
        return columns
    }

    /** Appends each of [missing] to whichever column currently has the fewest widgets. */
    private fun appendToShortestColumns(columns: List<List<String>>, missing: List<String>): List<List<String>> {
        val mutableColumns = columns.map { it.toMutableList() }.toMutableList()
        while (mutableColumns.size < 3) mutableColumns.add(mutableListOf())
        missing.forEach { id -> mutableColumns.minByOrNull { it.size }!!.add(id) }
        return mutableColumns
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

    /** Whether picking a Home background photo also sets it as the lock screen wallpaper. */
    val syncLockScreenWallpaper: Flow<Boolean> = context.dataStore.data.map { it[Keys.SYNC_LOCK_SCREEN_WALLPAPER] ?: true }

    suspend fun setSyncLockScreenWallpaper(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SYNC_LOCK_SCREEN_WALLPAPER] = enabled }
    }

    // ---------- Milestone / target (next mock test goal) ----------

    val milestoneTarget: Flow<MilestoneTarget> = context.dataStore.data.map { prefs ->
        prefs[Keys.MILESTONE_TARGET]
            ?.let { runCatching { json.decodeFromString<MilestoneTarget>(it) }.getOrNull() }
            ?: MilestoneTarget()
    }

    suspend fun setMilestoneTarget(target: MilestoneTarget) {
        context.dataStore.edit { it[Keys.MILESTONE_TARGET] = json.encodeToString(target) }
    }

    // ---------- App Shortcuts widget ----------

    val appShortcuts: Flow<List<AppShortcutRef>> = context.dataStore.data.map { prefs ->
        prefs[Keys.APP_SHORTCUTS]?.let { runCatching { json.decodeFromString<List<AppShortcutRef>>(it) }.getOrNull() } ?: emptyList()
    }

    suspend fun setAppShortcuts(shortcuts: List<AppShortcutRef>) {
        context.dataStore.edit { it[Keys.APP_SHORTCUTS] = json.encodeToString(shortcuts) }
    }

    // ---------- Recent Apps deck (see RecentAppsOverlay) ----------

    val recentApps: Flow<List<AppShortcutRef>> = context.dataStore.data.map { prefs ->
        prefs[Keys.RECENT_APPS]?.let { runCatching { json.decodeFromString<List<AppShortcutRef>>(it) }.getOrNull() } ?: emptyList()
    }

    /** Moves [ref] to the front of the recency list (or inserts it), trimmed to [MAX_RECENT_APPS]. */
    suspend fun recordAppLaunch(ref: AppShortcutRef) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.RECENT_APPS]
                ?.let { runCatching { json.decodeFromString<List<AppShortcutRef>>(it) }.getOrNull() }
                ?: emptyList()
            val updated = (listOf(ref) + current.filterNot { it == ref }).take(MAX_RECENT_APPS)
            prefs[Keys.RECENT_APPS] = json.encodeToString(updated)
        }
    }

    suspend fun removeFromRecentApps(ref: AppShortcutRef) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.RECENT_APPS]
                ?.let { runCatching { json.decodeFromString<List<AppShortcutRef>>(it) }.getOrNull() }
                ?: emptyList()
            prefs[Keys.RECENT_APPS] = json.encodeToString(current.filterNot { it == ref })
        }
    }

    // ---------- App Drawer categories ----------

    /** Package name -> [AppCategory]. Any app missing from this map is [AppCategory.OTHER]. */
    val appCategories: Flow<Map<String, AppCategory>> = context.dataStore.data.map { prefs ->
        prefs[Keys.APP_CATEGORIES]
            ?.let { runCatching { json.decodeFromString<Map<String, String>>(it) }.getOrNull() }
            ?.mapValues { (_, value) -> AppCategory.fromStorageValue(value) }
            ?: emptyMap()
    }

    suspend fun setAppCategory(packageName: String, category: AppCategory) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.APP_CATEGORIES]
                ?.let { runCatching { json.decodeFromString<Map<String, String>>(it) }.getOrNull() }
                ?.toMutableMap() ?: mutableMapOf()
            if (category == AppCategory.OTHER) current.remove(packageName) else current[packageName] = category.name
            prefs[Keys.APP_CATEGORIES] = json.encodeToString(current as Map<String, String>)
        }
    }

    // ---------- Double-tap to lock ----------

    val lockOnDoubleTap: Flow<Boolean> = context.dataStore.data.map { it[Keys.LOCK_ON_DOUBLE_TAP] ?: false }

    suspend fun setLockOnDoubleTap(enabled: Boolean) {
        context.dataStore.edit { it[Keys.LOCK_ON_DOUBLE_TAP] = enabled }
    }
}
