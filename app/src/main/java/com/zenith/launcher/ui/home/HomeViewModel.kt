package com.zenith.launcher.ui.home
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenith.launcher.data.model.AttentionProtectionMode
import com.zenith.launcher.data.model.AppCategory
import com.zenith.launcher.data.model.AppInfo
import com.zenith.launcher.data.model.AppShortcutRef
import com.zenith.launcher.data.model.BackgroundSettings
import com.zenith.launcher.data.model.BacklogItem
import com.zenith.launcher.data.model.BacklogUrgency
import com.zenith.launcher.data.model.DeadlineSettings
import com.zenith.launcher.data.model.DeadlineTarget
import com.zenith.launcher.data.model.StudyTarget
import com.zenith.launcher.data.model.LibraryLink
import com.zenith.launcher.data.model.TodoItem
import com.zenith.launcher.data.model.WidgetVisibility
import com.zenith.launcher.data.model.WidgetSize
import com.zenith.launcher.data.model.displayName
import com.zenith.launcher.data.repository.AppRepository
import com.zenith.launcher.data.repository.SettingsRepository
import com.zenith.launcher.util.DeadlineCountdownUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID


/** Everything the Home screen needs, combined into a single immutable snapshot. */
data class HomeUiState(
    val greeting: String = "",
    val apps: List<AppInfo> = emptyList(),
    val widgetVisibility: WidgetVisibility = WidgetVisibility(),
    /** The Home screen's 3-column grid arrangement - each entry is one column's ordered ids. */
    val widgetColumns: List<List<String>> = emptyList(),
    val phoneWidgetOrder: List<String> = emptyList(),
    val widgetSizes: Map<String, WidgetSize> = emptyMap(),
    val widgetHeights: Map<String, Int> = emptyMap(),
    val deadlineCountdowns: List<DeadlineCountdown> = emptyList(),
    val todoItems: List<TodoItem> = emptyList(),
    val backlogItems: List<BacklogItem> = emptyList(),
    val libraryLinks: List<LibraryLink> = emptyList(),
    val isFocusModeActive: Boolean = false,
    val focusAllowedApps: Set<String> = emptySet(),
    val distractionApps: Set<String> = emptySet(),
    val background: BackgroundSettings = BackgroundSettings(),
    val isLoadingApps: Boolean = true,
    val studyTargets: List<StudyTarget> = emptyList(),
    /** Resolved live from [appShortcutRefs] against currently-installed apps every time either changes. */
    val appShortcuts: List<AppInfo> = emptyList(),
    val appCategories: Map<String, String> = emptyMap(),
    val appCategoryTypes: List<String> = emptyList(),
    val lockOnDoubleTap: Boolean = false,
    val distractionPauseApp: AppInfo? = null,
    val distractionPauseSeconds: Int = 0,
    val distractionPauseMessage: String = "",
    val attentionProtectionMode: AttentionProtectionMode = AttentionProtectionMode.STRONG
)

/** One deadline's live countdown, derived from the user-managed deadline date. */
data class DeadlineCountdown(val id: String, val name: String, val daysLeft: Long?)

class HomeViewModel(
    private val appRepository: AppRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    private val _isLoadingApps = MutableStateFlow(true)
    private val _distractionPauseApp = MutableStateFlow<AppInfo?>(null)
    private val _distractionPauseSeconds = MutableStateFlow(0)
    private val _distractionPauseMessage = MutableStateFlow("")

    init {
        // Re-query immediately when the selected pack changes so both drawer and shortcuts use
        // the new drawables in the same UI update, rather than waiting for a later resume.
        viewModelScope.launch { settingsRepository.iconPackPackage.collect { loadApps() } }
    }

    /** Reloads installed apps, re-skinned with whichever icon pack is currently selected. */
    fun loadApps() {
        viewModelScope.launch {
            _isLoadingApps.value = true
            val iconPack = settingsRepository.iconPackPackage.first()
            val apps = appRepository.getInstalledApps(iconPack)
            settingsRepository.assignDefaultCategories(apps.associate { it.packageName to suggestedCategory(it) })
            _apps.value = apps
            _isLoadingApps.value = false
        }
    }

    private data class BaseSettings(
        val name: String,
        val deadlineSettings: DeadlineSettings,
        val visibility: WidgetVisibility,
        val columns: List<List<String>>,
        val phoneWidgetOrder: List<String>,
        val sizes: Map<String, WidgetSize>,
        val heights: Map<String, Int>,
        val focusActive: Boolean,
        val focusAllowedApps: Set<String>,
        val distractionApps: Set<String>,
        val background: BackgroundSettings,
        val studyTargets: List<StudyTarget>,
        val appShortcutRefs: List<AppShortcutRef>,
        val appCategories: Map<String, String>,
        val appCategoryTypes: List<String>,
        val lockOnDoubleTap: Boolean,
        val attentionProtectionMode: AttentionProtectionMode
    )

    private val baseState = settingsRepository.profileName.combine(settingsRepository.deadlineSettings) { name, deadlineSettings ->
        BaseSettings(name, deadlineSettings, WidgetVisibility(), emptyList(), emptyList(), emptyMap(), emptyMap(), false, emptySet(), emptySet(),
            BackgroundSettings(), emptyList(), emptyList(), emptyMap(), emptyList(), false, AttentionProtectionMode.STRONG)
    }.combine(settingsRepository.widgetVisibility) { base, visibility -> base.copy(visibility = visibility) }
        .combine(settingsRepository.widgetColumns) { base, columns -> base.copy(columns = columns) }
        .combine(settingsRepository.phoneWidgetOrder) { base, order -> base.copy(phoneWidgetOrder = order) }
        .combine(settingsRepository.widgetSizes) { base, sizes -> base.copy(sizes = sizes) }
        .combine(settingsRepository.widgetHeights) { base, heights -> base.copy(heights = heights) }
        .combine(settingsRepository.focusModeActive) { base, active -> base.copy(focusActive = active) }
        .combine(settingsRepository.focusAllowedApps) { base, allowed -> base.copy(focusAllowedApps = allowed) }
        .combine(settingsRepository.distractionApps) { base, distractions -> base.copy(distractionApps = distractions) }
        .combine(settingsRepository.backgroundSettings) { base, background -> base.copy(background = background) }
        .combine(settingsRepository.studyTargets) { base, targets -> base.copy(studyTargets = targets) }
        .combine(settingsRepository.appShortcuts) { base, shortcuts -> base.copy(appShortcutRefs = shortcuts) }
        .combine(settingsRepository.appCategories) { base, categories -> base.copy(appCategories = categories) }
        .combine(settingsRepository.appCategoryTypes) { base, types -> base.copy(appCategoryTypes = types) }
        .combine(settingsRepository.lockOnDoubleTap) { base, enabled -> base.copy(lockOnDoubleTap = enabled) }
        .combine(settingsRepository.attentionProtectionMode) { base, mode -> base.copy(attentionProtectionMode = mode) }

    private data class HomeContent(
        val base: BaseSettings,
        val apps: List<AppInfo> = emptyList(),
        val loading: Boolean = true,
        val todos: List<TodoItem> = emptyList(),
        val backlog: List<BacklogItem> = emptyList(),
        val library: List<LibraryLink> = emptyList()
    )

    private data class QuadHomeContent(
        val content: HomeContent,
        val pauseApp: AppInfo?,
        val pauseSeconds: Int,
        val pauseMessage: String
    )

    val uiState: StateFlow<HomeUiState> = baseState.combine(_apps) { base, apps -> HomeContent(base, apps = apps) }
        .combine(_isLoadingApps) { content, loading -> content.copy(loading = loading) }
        .combine(settingsRepository.todoList) { content, todos -> content.copy(todos = todos) }
        .combine(settingsRepository.backlogList) { content, backlog -> content.copy(backlog = backlog) }
        .combine(settingsRepository.libraryList) { content, library -> content.copy(library = library) }
        .combine(_distractionPauseApp) { content, pauseApp -> content to pauseApp }
        .combine(_distractionPauseSeconds) { pair, seconds -> Triple(pair.first, pair.second, seconds) }
        .combine(_distractionPauseMessage) { triple, message ->
            QuadHomeContent(triple.first, triple.second, triple.third, message)
        }
        .map { stateWithPause ->
        val content = stateWithPause.content

        val base = content.base
        val apps = content.apps
        val visibleApps = if (base.focusActive) apps.filter { it.packageName in base.focusAllowedApps } else apps
        val resolvedShortcuts = base.appShortcutRefs.mapNotNull { ref -> apps.find { it.packageName == ref.packageName && it.activityClassName == ref.activityClassName } }
        HomeUiState(
            greeting = buildGreeting(base.name), apps = visibleApps, widgetVisibility = base.visibility, widgetColumns = base.columns, phoneWidgetOrder = base.phoneWidgetOrder, widgetSizes = base.sizes, widgetHeights = base.heights,
            deadlineCountdowns = base.deadlineSettings.deadlines.map { deadline ->
                DeadlineCountdown(deadline.id, deadline.name, deadline.dateMillis?.let { DeadlineCountdownUtil.daysRemaining(it) })
            },
            todoItems = content.todos, backlogItems = content.backlog, libraryLinks = content.library,
            isFocusModeActive = base.focusActive, focusAllowedApps = base.focusAllowedApps, distractionApps = base.distractionApps,
            background = base.background, isLoadingApps = content.loading,
            studyTargets = base.studyTargets, appShortcuts = resolvedShortcuts,
            appCategories = base.appCategories, appCategoryTypes = base.appCategoryTypes, lockOnDoubleTap = base.lockOnDoubleTap, attentionProtectionMode = base.attentionProtectionMode,
            distractionPauseApp = stateWithPause.pauseApp, distractionPauseSeconds = stateWithPause.pauseSeconds, distractionPauseMessage = stateWithPause.pauseMessage
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    /** Chooses a fresh greeting when the profile name changes. */
    private fun buildGreeting(name: String): String {
        val dayOfYear = LocalDate.now().dayOfYear
        return ZenithCopy.greetings.random().format(name)
    }

    fun launchApp(app: AppInfo) {
        val isDistraction = app.packageName in uiState.value.distractionApps
        if (!isDistraction) {
            performLaunch(app)
            return
        }

        val message = ZenithCopy.distractionPause.random()
        _distractionPauseApp.value = app
        _distractionPauseSeconds.value = 7
        _distractionPauseMessage.value = message
        viewModelScope.launch {
            while (_distractionPauseSeconds.value > 0 && _distractionPauseApp.value === app) {
                kotlinx.coroutines.delay(1000)
                _distractionPauseSeconds.value = (_distractionPauseSeconds.value - 1).coerceAtLeast(0)
            }
            if (_distractionPauseApp.value === app) {
                _distractionPauseApp.value = null
                _distractionPauseMessage.value = ""
                performLaunch(app)
            }
        }
    }

    fun cancelDistractionPause() {
        _distractionPauseApp.value = null
        _distractionPauseSeconds.value = 0
        _distractionPauseMessage.value = ""
    }

    private fun performLaunch(app: AppInfo) {
        appRepository.launchApp(app.packageName, app.activityClassName)
        viewModelScope.launch {
        }
    }

    // ---------- Focus mode ----------
    fun toggleFocusMode() = viewModelScope.launch {
        settingsRepository.setFocusModeActive(!uiState.value.isFocusModeActive)
    }

    // ---------- Widget drag-to-reorder (3-column grid) ----------
    /**
     * Moves widget [id] out of whichever column currently holds it and re-inserts it at
     * [toIndex] within [toColumn], then persists the new arrangement. Works for both a plain
     * reorder within one column and a drag across columns - both are just "remove, then insert
     * elsewhere" on the same column list.
     */
    /** Reorders the phone's single-column layout without changing the tablet three-column layout. */
    fun movePhoneWidget(id: String, toIndex: Int) {
        val current = uiState.value.phoneWidgetOrder.toMutableList()
        val fromIndex = current.indexOf(id)
        if (fromIndex == -1) return
        current.removeAt(fromIndex)
        current.add(toIndex.coerceIn(0, current.size), id)
        viewModelScope.launch { settingsRepository.setPhoneWidgetOrder(current) }
    }

    fun moveWidget(id: String, toColumn: Int, toIndex: Int) {
        val current = uiState.value.widgetColumns.map { it.toMutableList() }.toMutableList()
        if (toColumn !in current.indices) return

        val fromColumn = current.indexOfFirst { it.contains(id) }
        if (fromColumn == -1) return
        current[fromColumn].remove(id)

        val target = current[toColumn]
        target.add(toIndex.coerceIn(0, target.size), id)

        viewModelScope.launch { settingsRepository.setWidgetColumns(current) }
    }

    fun cycleWidgetSize(id: String) = viewModelScope.launch {
        val current = uiState.value.widgetSizes[id] ?: WidgetSize.STANDARD
        settingsRepository.setWidgetSize(id, current.next())
    }

    fun adjustWidgetHeight(id: String, deltaDp: Int) = viewModelScope.launch {
        val legacyHeight = (uiState.value.widgetSizes[id] ?: WidgetSize.STANDARD).minHeightDp
        val minimum = if (id == com.zenith.launcher.data.model.WidgetIds.DEADLINES) 188 else 88
        settingsRepository.setWidgetHeight(id, ((uiState.value.widgetHeights[id] ?: legacyHeight) + deltaDp).coerceIn(minimum, 600))
    }

    /**
     * Automatic drawer categorisation. It combines app labels/package names with Android's
     * coarse application category so common apps do not fall into one giant "Other" bucket.
     * "Other" is now only the final fallback for genuinely unclassifiable apps.
     */
    private fun suggestedCategory(app: AppInfo): String {
        val identity = "${app.packageName} ${app.label}".lowercase()

        return when {
            // Study / education
            listOf(
                "allen", "classroom", "docs", "sheets", "slides", "notion", "khan",
                "coursera", "udemy", "unacademy", "byju", "physicswallah", "pw", "doubtnut",
                "vedantu", "study", "exam", "learn", "education", "quiz", "anki"
            ).any(identity::contains) -> AppCategory.STUDY.displayName

            // Social / communication
            listOf(
                "whatsapp", "telegram", "instagram", "facebook", "snapchat", "discord",
                "messenger", "linkedin", "twitter", "x.com", "reddit", "threads",
                "signal", "wechat", "skype"
            ).any(identity::contains) -> AppCategory.SOCIAL.displayName

            // Entertainment
            listOf(
                "youtube", "netflix", "primevideo", "prime video", "hotstar", "spotify",
                "music", "video", "tv", "twitch", "jio cinema", "sonyliv", "mx player",
                "vlc", "podcast"
            ).any(identity::contains) -> AppCategory.ENTERTAINMENT.displayName

            // Games
            listOf(
                "game", "games", "supercell", "roblox", "minecraft", "pubg", "bgmi",
                "freefire", "free fire", "cod mobile", "call of duty", "clash of",
                "brawl stars", "genshin"
            ).any(identity::contains) -> AppCategory.GAMES.displayName

            // Purpose-specific categories. These are built-in so the drawer stays useful
            // without requiring the user to manually create categories.
            listOf(
                "gmail", "outlook", "mail", "email", "protonmail", "contacts",
                "phone", "messages", "messaging", "sms"
            ).any(identity::contains) -> "Communication"

            listOf(
                "camera", "gallery", "photos", "google photos", "lightroom", "snapseed",
                "picsart", "image", "audio", "sound", "recorder", "gallery"
            ).any(identity::contains) ||
                app.androidCategory == android.content.pm.ApplicationInfo.CATEGORY_IMAGE ||
                app.androidCategory == android.content.pm.ApplicationInfo.CATEGORY_AUDIO ||
                app.androidCategory == android.content.pm.ApplicationInfo.CATEGORY_VIDEO -> "Media"

            // Cloud storage and file management - split out from Utilities/Study so it has its
            // own drawer bucket rather than being buried under either.
            listOf(
                "drive", "google drive", "onedrive", "dropbox", "mega", "cloud", "storage",
                "files", "file manager", "file explorer", "finder", "solid explorer",
                "es file explorer", "sd card"
            ).any(identity::contains) -> "Storage"

            listOf(
                "calculator", "calendar", "clock", "alarm", "timer", "notes", "keep",
                "todo", "tasks", "weather", "device care", "cleaner", "settings"
            ).any(identity::contains) -> "Utilities"

            listOf(
                "chrome", "browser", "firefox", "brave", "edge", "opera", "internet",
                "search", "duckduckgo"
            ).any(identity::contains) -> "Internet"

            listOf(
                "github", "gitlab", "termux", "code", "ide", "developer", "android studio"
            ).any(identity::contains) -> "Development"

            listOf(
                "amazon", "flipkart", "myntra", "meesho", "shop", "store", "ebay",
                "etsy", "shopping"
            ).any(identity::contains) -> "Shopping"

            listOf(
                "bank", "banking", "pay", "payment", "wallet", "upi", "phonepe",
                "gpay", "google pay", "paytm", "cred"
            ).any(identity::contains) -> "Finance"

            listOf(
                "maps", "map", "uber", "ola", "rapido", "metro", "flight", "travel",
                "booking", "airbnb"
            ).any(identity::contains) ||
                app.androidCategory == android.content.pm.ApplicationInfo.CATEGORY_MAPS -> "Travel"

            listOf(
                "news", "newsstand", "times of india", "reddit"
            ).any(identity::contains) -> "News"

            // Android's own coarse category is useful when an app has an unfamiliar name.
            app.androidCategory == android.content.pm.ApplicationInfo.CATEGORY_GAME -> AppCategory.GAMES.displayName
            app.androidCategory == android.content.pm.ApplicationInfo.CATEGORY_SOCIAL -> AppCategory.SOCIAL.displayName
            app.androidCategory == android.content.pm.ApplicationInfo.CATEGORY_PRODUCTIVITY -> "Productivity"

            else -> AppCategory.OTHER.displayName
        }
    }

    // ---------- Daily to-do ----------
    fun addTodo(text: String) = viewModelScope.launch {
        if (text.isBlank()) return@launch
        val updated = uiState.value.todoItems + TodoItem(id = UUID.randomUUID().toString(), text = text.trim())
        settingsRepository.setTodoList(updated)
    }

    fun updateTodo(item: TodoItem) = viewModelScope.launch {
        if (item.text.isBlank()) return@launch
        settingsRepository.setTodoList(uiState.value.todoItems.map { if (it.id == item.id) item.copy(text = item.text.trim()) else it })
    }

    fun toggleTodo(id: String) = viewModelScope.launch {
        val updated = uiState.value.todoItems.map { if (it.id == id) it.copy(isDone = !it.isDone) else it }
        settingsRepository.setTodoList(updated)
    }

    fun deleteTodo(id: String) = viewModelScope.launch {
        settingsRepository.setTodoList(uiState.value.todoItems.filterNot { it.id == id })
    }

    // ---------- Deadlines ----------
    fun addDeadline(name: String, dateMillis: Long?) = viewModelScope.launch {
        if (name.isBlank()) return@launch
        val item = DeadlineTarget(id = UUID.randomUUID().toString(), name = name.trim(), dateMillis = dateMillis)
        // Re-read the persisted list so the operation remains safe if another surface changed it.
        val deadlines = settingsRepository.deadlineSettings.first().deadlines + item
        settingsRepository.setDeadlineSettings(DeadlineSettings(deadlines))
    }

    // ---------- Backlog ----------
    fun addBacklogItem(subject: String, itemName: String, urgency: BacklogUrgency) = viewModelScope.launch {
        if (itemName.isBlank()) return@launch
        val item = BacklogItem(id = UUID.randomUUID().toString(), subject = subject, itemName = itemName.trim(), urgency = urgency)
        settingsRepository.setBacklogList(uiState.value.backlogItems + item)
    }

    fun updateBacklogItem(item: BacklogItem) = viewModelScope.launch {
        if (item.itemName.isBlank()) return@launch
        settingsRepository.setBacklogList(uiState.value.backlogItems.map { if (it.id == item.id) item.copy(itemName = item.itemName.trim(), subject = item.subject.trim().ifBlank { "General" }) else it })
    }

    fun deleteBacklogItem(id: String) = viewModelScope.launch {
        settingsRepository.setBacklogList(uiState.value.backlogItems.filterNot { it.id == id })
    }

    // ---------- Library ----------
    fun addLibraryLink(title: String, uriString: String) = viewModelScope.launch {
        if (title.isBlank() || uriString.isBlank()) return@launch
        val link = LibraryLink(id = UUID.randomUUID().toString(), title = title.trim(), uriString = uriString)
        settingsRepository.setLibraryList(uiState.value.libraryLinks + link)
    }

    fun deleteLibraryLink(id: String) = viewModelScope.launch {
        settingsRepository.setLibraryList(uiState.value.libraryLinks.filterNot { it.id == id })
    }

    // ---------- Study targets ----------
    fun addStudyTarget() = viewModelScope.launch {
        val target = StudyTarget(id = UUID.randomUUID().toString())
        settingsRepository.setStudyTargets(uiState.value.studyTargets + target)
    }

    fun updateStudyTarget(target: StudyTarget) = viewModelScope.launch {
        val updated = uiState.value.studyTargets.map { if (it.id == target.id) target else it }
        settingsRepository.setStudyTargets(updated)
    }

    fun deleteStudyTarget(id: String) = viewModelScope.launch {
        settingsRepository.setStudyTargets(uiState.value.studyTargets.filterNot { it.id == id })
    }

    // ---------- App shortcuts ----------
    fun addAppShortcut(app: AppInfo) = viewModelScope.launch {
        val current = settingsRepository.appShortcuts.first()
        if (current.any { it.packageName == app.packageName && it.activityClassName == app.activityClassName }) return@launch
        settingsRepository.setAppShortcuts(current + AppShortcutRef(app.packageName, app.activityClassName))
    }

    fun removeAppShortcut(app: AppInfo) = viewModelScope.launch {
        val current = settingsRepository.appShortcuts.first()
        settingsRepository.setAppShortcuts(current.filterNot {
            it.packageName == app.packageName && it.activityClassName == app.activityClassName
        })
    }

    // ---------- App Drawer + attention classification ----------
    fun setAppCategory(app: AppInfo, category: String) = viewModelScope.launch {
        settingsRepository.setAppCategory(app.packageName, category)
    }

    fun toggleFocusAllowedApp(app: AppInfo) = viewModelScope.launch {
        val allowed = uiState.value.focusAllowedApps
        val next = if (app.packageName in allowed) allowed - app.packageName else allowed + app.packageName
        settingsRepository.setFocusAllowedApps(next)
        if (app.packageName !in allowed) {
            settingsRepository.setDistractionApps(uiState.value.distractionApps - app.packageName)
        }
    }

    fun toggleDistractionApp(app: AppInfo) = viewModelScope.launch {
        val distractions = uiState.value.distractionApps
        val next = if (app.packageName in distractions) distractions - app.packageName else distractions + app.packageName
        settingsRepository.setDistractionApps(next)
        if (app.packageName !in distractions) {
            settingsRepository.setFocusAllowedApps(uiState.value.focusAllowedApps - app.packageName)
        }
    }

    // ---------- Uninstall / app info ----------
    /** Only apps not flagged as system apps can be uninstalled - matches Android's own rule. */
    fun canUninstall(app: AppInfo): Boolean = !app.isSystemApp
    fun uninstallApp(app: AppInfo) = appRepository.uninstallApp(app.packageName)
    fun openAppInfo(app: AppInfo) = appRepository.openAppInfo(app.packageName)
}
