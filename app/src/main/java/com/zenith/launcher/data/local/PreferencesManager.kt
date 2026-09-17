package com.zenith.launcher.data.local
import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.zenith.launcher.data.model.AttentionProtectionMode
import com.zenith.launcher.data.model.AppCategory
import com.zenith.launcher.data.model.defaultAppCategoryTypes
import com.zenith.launcher.data.model.displayName
import com.zenith.launcher.data.model.AppShortcutRef
import com.zenith.launcher.data.model.BackgroundSettings
import com.zenith.launcher.data.model.BacklogItem
import com.zenith.launcher.data.model.DeadlineSettings
import com.zenith.launcher.data.model.FontChoice
import com.zenith.launcher.data.model.StudyTarget
import com.zenith.launcher.data.model.LibraryLink
import com.zenith.launcher.data.model.TodoItem
import com.zenith.launcher.data.model.WidgetIds
import com.zenith.launcher.data.model.WidgetVisibility
import com.zenith.launcher.data.model.WidgetSize
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive


/** Single top-level DataStore instance for the whole app process. The on-disk name stays stable for upgrades. */
private val Context.dataStore by preferencesDataStore(name = "launcher_settings")

/**
 * Central read/write point for every persisted launcher setting. Each setting is exposed as a
 * Flow so ViewModels collect it reactively and the UI recomposes automatically on change.
 *
 * Lists (tasks/backlog/library links/allowed apps/widget order) are stored as a single JSON string
 * per key, since DataStore Preferences only supports primitive types natively.
 */
class PreferencesManager(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    private object Keys {
        val PROFILE_NAME = stringPreferencesKey("profile_name")
        val DEADLINE_SETTINGS = stringPreferencesKey("exam_settings_json")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val FONT_CHOICE = stringPreferencesKey("font_choice")
        val CUSTOM_FONT_PATH = stringPreferencesKey("custom_font_path")
        val ICON_PACK_PACKAGE = stringPreferencesKey("icon_pack_package")
        val WIDGET_VISIBILITY = stringPreferencesKey("widget_visibility_json")
        val WIDGET_ORDER = stringPreferencesKey("widget_order_json")
        val PHONE_WIDGET_ORDER = stringPreferencesKey("phone_widget_order_json")
        val WIDGET_SIZES = stringPreferencesKey("widget_sizes_json")
        val WIDGET_HEIGHTS = stringPreferencesKey("widget_heights_json")
        val WIDGET_HEIGHTS_VERSION = stringPreferencesKey("widget_heights_version")
        val TODO_LIST = stringPreferencesKey("todo_list_json")
        val BACKLOG_LIST = stringPreferencesKey("chapter_list_json")
        val LIBRARY_LIST = stringPreferencesKey("pdf_list_json")
        val FOCUS_MODE_ACTIVE = booleanPreferencesKey("focus_mode_active")
        // Renamed from the old "blocked apps" key: Focus Mode is now an allow-list (pick the
        // apps you WANT visible while focused), so this deliberately doesn't reuse the old key.
        val FOCUS_ALLOWED_APPS = stringPreferencesKey("focus_allowed_apps_json")
        val DISTRACTION_APPS = stringPreferencesKey("distraction_apps_json")
        val BACKGROUND_IMAGE_URI = stringPreferencesKey("background_image_uri")
        val BACKGROUND_COLOR = longPreferencesKey("background_color_argb")
        val SYNC_LOCK_SCREEN_WALLPAPER = booleanPreferencesKey("sync_lock_screen_wallpaper")
        val STUDY_TARGETS = stringPreferencesKey("milestone_target_json")
        val APP_SHORTCUTS = stringPreferencesKey("app_shortcuts_json")
        val APP_CATEGORIES = stringPreferencesKey("app_categories_json")
        val APP_CATEGORY_TYPES = stringPreferencesKey("app_category_types_json")
        val APP_CATEGORY_AUTOFILL_VERSION = stringPreferencesKey("app_category_autofill_version")
        val LOCK_ON_DOUBLE_TAP = booleanPreferencesKey("lock_on_double_tap")
        val ATTENTION_PROTECTION_MODE = stringPreferencesKey("attention_protection_mode")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    // ---------- Onboarding ----------

    val onboardingCompleted: Flow<Boolean> = context.dataStore.data.map { it[Keys.ONBOARDING_COMPLETED] ?: false }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    // ---------- Profile ----------

    val profileName: Flow<String> = context.dataStore.data.map { it[Keys.PROFILE_NAME] ?: "Student" }

    suspend fun setProfileName(name: String) {
        context.dataStore.edit { it[Keys.PROFILE_NAME] = name }
    }

    // ---------- Deadlines (fully user-managed list) ----------

    val deadlineSettings: Flow<DeadlineSettings> = context.dataStore.data.map { prefs ->
        prefs[Keys.DEADLINE_SETTINGS]?.let { runCatching { json.decodeFromString<DeadlineSettings>(it) }.getOrNull() } ?: DeadlineSettings()
    }

    suspend fun setDeadlineSettings(settings: DeadlineSettings) {
        context.dataStore.edit { it[Keys.DEADLINE_SETTINGS] = json.encodeToString(settings) }
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

    /** Absolute path in app-internal storage to a user-imported font file. */
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

    /** Separate phone order so switching between phone and tablet never rewrites the other layout. */
    val phoneWidgetOrder: Flow<List<String>> = context.dataStore.data.map { prefs ->
        val raw = prefs[Keys.PHONE_WIDGET_ORDER]
        val saved = raw?.let { runCatching { json.decodeFromString<List<String>>(it) }.getOrNull() }
        val base = saved ?: WidgetIds.DEFAULT_ORDER
        val visible = base.filter { it in WidgetIds.DEFAULT_ORDER }
        val missing = WidgetIds.DEFAULT_ORDER.filterNot { it in visible }
        visible + missing
    }

    suspend fun setPhoneWidgetOrder(order: List<String>) {
        context.dataStore.edit { it[Keys.PHONE_WIDGET_ORDER] = json.encodeToString(order) }
    }

    val widgetSizes: Flow<Map<String, WidgetSize>> = context.dataStore.data.map { prefs ->
        prefs[Keys.WIDGET_SIZES]
            ?.let { runCatching { json.decodeFromString<Map<String, WidgetSize>>(it) }.getOrNull() }
            ?: emptyMap()
    }

    suspend fun setWidgetSize(id: String, size: WidgetSize) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.WIDGET_SIZES]
                ?.let { runCatching { json.decodeFromString<Map<String, WidgetSize>>(it) }.getOrNull() }
                ?.toMutableMap() ?: mutableMapOf()
            current[id] = size
            prefs[Keys.WIDGET_SIZES] = json.encodeToString(current)
        }
    }

    /** User-selected widget heights in dp; separate from legacy size presets for migration. */
    val widgetHeights: Flow<Map<String, Int>> = context.dataStore.data.map { prefs ->
        if (prefs[Keys.WIDGET_HEIGHTS_VERSION] != "3") {
            WidgetIds.DEFAULT_HEIGHTS
        } else {
            prefs[Keys.WIDGET_HEIGHTS]
                ?.let { runCatching { json.decodeFromString<Map<String, Int>>(it) }.getOrNull() }
                ?: WidgetIds.DEFAULT_HEIGHTS
        }
    }

    suspend fun setWidgetHeight(id: String, heightDp: Int) {
        context.dataStore.edit { prefs ->
            // Version 3 deliberately starts from the current reference defaults. Older installs
            // may contain v2 heights, but those should not be resurrected when the new compact
            // layout is introduced. Once the user resizes anything, their values are persisted.
            val current = if (prefs[Keys.WIDGET_HEIGHTS_VERSION] == "3") {
                prefs[Keys.WIDGET_HEIGHTS]
                    ?.let { runCatching { json.decodeFromString<Map<String, Int>>(it) }.getOrNull() }
                    ?.toMutableMap()
                    ?: WidgetIds.DEFAULT_HEIGHTS.toMutableMap()
            } else {
                WidgetIds.DEFAULT_HEIGHTS.toMutableMap()
            }
            current[id] = heightDp.coerceIn(88, 600)
            prefs[Keys.WIDGET_HEIGHTS] = json.encodeToString(current)
            prefs[Keys.WIDGET_HEIGHTS_VERSION] = "3"
        }
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

    // ---------- Todo list ----------

    val todoList: Flow<List<TodoItem>> = context.dataStore.data.map { prefs ->
        prefs[Keys.TODO_LIST]?.let { runCatching { json.decodeFromString<List<TodoItem>>(it) }.getOrNull() } ?: emptyList()
    }

    suspend fun setTodoList(items: List<TodoItem>) {
        context.dataStore.edit { it[Keys.TODO_LIST] = json.encodeToString(items) }
    }

    // ---------- Backlog ----------

    val backlogList: Flow<List<BacklogItem>> = context.dataStore.data.map { prefs ->
        prefs[Keys.BACKLOG_LIST]
            ?.let { runCatching { json.decodeFromString<List<BacklogItem>>(migrateBacklogUrgencyJson(it)) }.getOrNull() }
            ?: emptyList()
    }

    suspend fun setBacklogList(items: List<BacklogItem>) {
        context.dataStore.edit { it[Keys.BACKLOG_LIST] = json.encodeToString(items) }
    }

    /**
     * Pre-urgency installs stored each chapter's priority as a "status" enum (PENDING/REVISION/
     * WEAK_AREA) rather than today's "urgency" (LOW/MEDIUM/HIGH). Rewrites that field before
     * decoding rather than just letting it fall back to the modern default for every old item,
     * so the old signal isn't thrown away: a weak area is the most urgent to revisit (HIGH), a
     * item already flagged for revision is the least urgent of the three (LOW), and a merely
     * pending item sits in between (MEDIUM, also the modern default). Anything already in the
     * current shape (no "status" key) - or anything this can't parse at all - passes through
     * unchanged, and the caller's own `runCatching` is the final safety net.
     */
    private fun migrateBacklogUrgencyJson(raw: String): String = runCatching {
        val items = json.parseToJsonElement(raw).jsonArray.map { element ->
            val obj = element.jsonObject
            val legacyStatus = obj["status"]?.jsonPrimitive?.contentOrNull
            if (legacyStatus == null) {
                element
            } else {
                val urgency = when (legacyStatus) {
                    "WEAK_AREA" -> "HIGH"
                    "REVISION" -> "LOW"
                    else -> "MEDIUM"
                }
                JsonObject(obj.toMutableMap().apply { remove("status"); put("urgency", JsonPrimitive(urgency)) })
            }
        }
        JsonArray(items).toString()
    }.getOrDefault(raw)

    // ---------- Library links ----------

    val libraryList: Flow<List<LibraryLink>> = context.dataStore.data.map { prefs ->
        prefs[Keys.LIBRARY_LIST]?.let { runCatching { json.decodeFromString<List<LibraryLink>>(it) }.getOrNull() } ?: emptyList()
    }

    suspend fun setLibraryList(items: List<LibraryLink>) {
        context.dataStore.edit { it[Keys.LIBRARY_LIST] = json.encodeToString(items) }
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

    /** Apps the user identifies as distractions. Launching them gets a short reflective pause. */
    val distractionApps: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs[Keys.DISTRACTION_APPS]
            ?.let { runCatching { json.decodeFromString<List<String>>(it) }.getOrNull() }
            ?.toSet() ?: emptySet()
    }

    suspend fun setDistractionApps(packages: Set<String>) {
        context.dataStore.edit { it[Keys.DISTRACTION_APPS] = json.encodeToString(packages.toList()) }
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

    // ---------- Study targets ----------

    /**
     * Tries the current `List<StudyTarget>` shape first; if that fails, falls back to
     * decoding a single pre-multi-target `StudyTarget` object (the shape this key held
     * before multiple targets were supported) and wraps it in a one-item list, so upgrading
     * doesn't lose an existing target. [StudyTarget.id] defaults to "default" specifically
     * so that old JSON - which predates the id field - still decodes successfully here.
     */
    val studyTargets: Flow<List<StudyTarget>> = context.dataStore.data.map { prefs ->
        val raw = prefs[Keys.STUDY_TARGETS]
        raw?.let { runCatching { json.decodeFromString<List<StudyTarget>>(it) }.getOrNull() }
            ?: raw?.let { runCatching { json.decodeFromString<StudyTarget>(it) }.getOrNull() }?.let { listOf(it) }
            ?: emptyList()
    }

    suspend fun setStudyTargets(targets: List<StudyTarget>) {
        context.dataStore.edit { it[Keys.STUDY_TARGETS] = json.encodeToString(targets) }
    }

    // ---------- App Shortcuts widget ----------

    val appShortcuts: Flow<List<AppShortcutRef>> = context.dataStore.data.map { prefs ->
        prefs[Keys.APP_SHORTCUTS]?.let { runCatching { json.decodeFromString<List<AppShortcutRef>>(it) }.getOrNull() } ?: emptyList()
    }

    suspend fun setAppShortcuts(shortcuts: List<AppShortcutRef>) {
        context.dataStore.edit { it[Keys.APP_SHORTCUTS] = json.encodeToString(shortcuts) }
    }

    // ---------- App Drawer categories ----------

    /** Package name -> drawer category, including an explicit initial suggestion for every app. */
    val appCategories: Flow<Map<String, String>> = context.dataStore.data.map { prefs ->
        prefs[Keys.APP_CATEGORIES]
            ?.let { runCatching { json.decodeFromString<Map<String, String>>(it) }.getOrNull() }
            ?.mapValues { (_, value) -> AppCategory.entries.firstOrNull { it.name == value }?.displayName ?: value }
            ?: emptyMap()
    }

    /** User-defined labels extend the built-in drawer categories. */
    val appCategoryTypes: Flow<List<String>> = context.dataStore.data.map { prefs ->
        val saved = prefs[Keys.APP_CATEGORY_TYPES]
            ?.let { runCatching { json.decodeFromString<List<String>>(it) }.getOrNull() }
            ?: emptyList()
        (defaultAppCategoryTypes + saved).distinct()
    }

    suspend fun addAppCategoryType(label: String) {
        val normalized = label.trim().replaceFirstChar { it.uppercase() }
        if (normalized.isBlank() || normalized in defaultAppCategoryTypes) return
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.APP_CATEGORY_TYPES]
                ?.let { runCatching { json.decodeFromString<List<String>>(it) }.getOrNull() }
                ?: emptyList()
            prefs[Keys.APP_CATEGORY_TYPES] = json.encodeToString((current + normalized).distinct())
        }
    }

    suspend fun setAppCategory(packageName: String, category: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.APP_CATEGORIES]
                ?.let { runCatching { json.decodeFromString<Map<String, String>>(it) }.getOrNull() }
                ?.toMutableMap() ?: mutableMapOf()
            current[packageName] = category
            prefs[Keys.APP_CATEGORIES] = json.encodeToString(current as Map<String, String>)
        }
    }

    /**
     * Gives newly discovered launcher apps a real persisted category immediately.  Keeping the
     * default explicit (rather than only deriving it in the UI) makes category management
     * predictable while preserving every later user assignment.
     */
    suspend fun assignDefaultCategories(defaults: Map<String, String>) {
        if (defaults.isEmpty()) return
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.APP_CATEGORIES]
                ?.let { runCatching { json.decodeFromString<Map<String, String>>(it) }.getOrNull() }
                ?.toMutableMap() ?: mutableMapOf()

            // One-time migration: apps that were previously dumped into "Other" get the
            // improved automatic classification. Once migrated, manual category choices remain
            // untouched, including a deliberate choice to use "Other".
            val needsMigration = prefs[Keys.APP_CATEGORY_AUTOFILL_VERSION] != "2"
            defaults.forEach { (packageName, category) ->
                if (!current.containsKey(packageName) ||
                    (needsMigration && current[packageName] == AppCategory.OTHER.displayName)
                ) {
                    current[packageName] = category
                }
            }

            prefs[Keys.APP_CATEGORIES] = json.encodeToString(current)
            prefs[Keys.APP_CATEGORY_AUTOFILL_VERSION] = "2"

            // Any category an app actually ends up in - built-in or not - is registered as its
            // own selectable drawer group, the same as one typed in manually via Settings > "Add
            // category". Without this, a category reachable only through auto-assignment (never
            // through the manual add-category flow) would have apps living in it but no entry in
            // the type list itself.
            val savedTypes = prefs[Keys.APP_CATEGORY_TYPES]
                ?.let { runCatching { json.decodeFromString<List<String>>(it) }.getOrNull() }
                ?: emptyList()
            val knownTypes = defaultAppCategoryTypes.toSet() + savedTypes
            val newTypes = current.values.filterNot { it in knownTypes }.distinct()
            if (newTypes.isNotEmpty()) {
                prefs[Keys.APP_CATEGORY_TYPES] = json.encodeToString((savedTypes + newTypes).distinct())
            }
        }
    }

    // ---------- Double-tap to lock ----------

    val lockOnDoubleTap: Flow<Boolean> = context.dataStore.data.map { it[Keys.LOCK_ON_DOUBLE_TAP] ?: false }

    suspend fun setLockOnDoubleTap(enabled: Boolean) {
        context.dataStore.edit { it[Keys.LOCK_ON_DOUBLE_TAP] = enabled }
    }

    val attentionProtectionMode: Flow<AttentionProtectionMode> =
        context.dataStore.data.map { AttentionProtectionMode.fromStorageValue(it[Keys.ATTENTION_PROTECTION_MODE]) }

    suspend fun setAttentionProtectionMode(mode: AttentionProtectionMode) {
        context.dataStore.edit { it[Keys.ATTENTION_PROTECTION_MODE] = mode.storageValue }
    }
}
