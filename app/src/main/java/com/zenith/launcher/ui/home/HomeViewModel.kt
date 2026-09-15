package com.zenith.launcher.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenith.launcher.data.model.AppCategory
import com.zenith.launcher.data.model.AppInfo
import com.zenith.launcher.data.model.AppShortcutRef
import com.zenith.launcher.data.model.BackgroundSettings
import com.zenith.launcher.data.model.ChapterItem
import com.zenith.launcher.data.model.ChapterStatus
import com.zenith.launcher.data.model.ExamSettings
import com.zenith.launcher.data.model.MilestoneTarget
import com.zenith.launcher.data.model.PdfLink
import com.zenith.launcher.data.model.TodoItem
import com.zenith.launcher.data.model.WidgetVisibility
import com.zenith.launcher.data.model.WidgetSize
import com.zenith.launcher.data.model.displayName
import com.zenith.launcher.data.repository.AppRepository
import com.zenith.launcher.data.repository.SettingsRepository
import com.zenith.launcher.util.CountdownUtil
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
    val widgetSizes: Map<String, WidgetSize> = emptyMap(),
    val widgetHeights: Map<String, Int> = emptyMap(),
    val widgetWidths: Map<String, Int> = emptyMap(),
    val examCountdowns: List<ExamCountdown> = emptyList(),
    val todoItems: List<TodoItem> = emptyList(),
    val chapterItems: List<ChapterItem> = emptyList(),
    val pdfLinks: List<PdfLink> = emptyList(),
    val isFocusModeActive: Boolean = false,
    val background: BackgroundSettings = BackgroundSettings(),
    val isLoadingApps: Boolean = true,
    val milestoneTarget: MilestoneTarget = MilestoneTarget(),
    /** Resolved live from [appShortcutRefs] against currently-installed apps every time either changes. */
    val appShortcuts: List<AppInfo> = emptyList(),
    /** Same resolution rule as [appShortcuts], newest-launched first. */
    val recentApps: List<AppInfo> = emptyList(),
    val appCategories: Map<String, String> = emptyMap(),
    val appCategoryTypes: List<String> = emptyList(),
    val lockOnDoubleTap: Boolean = false
)

/** One exam's live countdown, derived each recomposition from [com.zenith.launcher.data.model.ExamTarget]. */
data class ExamCountdown(val id: String, val name: String, val daysLeft: Long?)

class HomeViewModel(
    private val appRepository: AppRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    private val _isLoadingApps = MutableStateFlow(true)

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
        val exam: ExamSettings,
        val visibility: WidgetVisibility,
        val columns: List<List<String>>,
        val sizes: Map<String, WidgetSize>,
        val heights: Map<String, Int>,
        val widths: Map<String, Int>,
        val focusActive: Boolean,
        val allowedApps: Set<String>,
        val background: BackgroundSettings,
        val milestoneTarget: MilestoneTarget,
        val appShortcutRefs: List<AppShortcutRef>,
        val recentAppRefs: List<AppShortcutRef>,
        val appCategories: Map<String, String>,
        val appCategoryTypes: List<String>,
        val lockOnDoubleTap: Boolean
    )

    private val baseState = settingsRepository.profileName.combine(settingsRepository.examSettings) { name, exam ->
        BaseSettings(name, exam, WidgetVisibility(), emptyList(), emptyMap(), emptyMap(), emptyMap(), false, emptySet(),
            BackgroundSettings(), MilestoneTarget(), emptyList(), emptyList(), emptyMap(), emptyList(), false)
    }.combine(settingsRepository.widgetVisibility) { base, visibility -> base.copy(visibility = visibility) }
        .combine(settingsRepository.widgetColumns) { base, columns -> base.copy(columns = columns) }
        .combine(settingsRepository.widgetSizes) { base, sizes -> base.copy(sizes = sizes) }
        .combine(settingsRepository.widgetHeights) { base, heights -> base.copy(heights = heights) }
        .combine(settingsRepository.widgetWidths) { base, widths -> base.copy(widths = widths) }
        .combine(settingsRepository.focusModeActive) { base, active -> base.copy(focusActive = active) }
        .combine(settingsRepository.focusAllowedApps) { base, allowed -> base.copy(allowedApps = allowed) }
        .combine(settingsRepository.backgroundSettings) { base, background -> base.copy(background = background) }
        .combine(settingsRepository.milestoneTarget) { base, target -> base.copy(milestoneTarget = target) }
        .combine(settingsRepository.appShortcuts) { base, shortcuts -> base.copy(appShortcutRefs = shortcuts) }
        .combine(settingsRepository.recentApps) { base, recents -> base.copy(recentAppRefs = recents) }
        .combine(settingsRepository.appCategories) { base, categories -> base.copy(appCategories = categories) }
        .combine(settingsRepository.appCategoryTypes) { base, types -> base.copy(appCategoryTypes = types) }
        .combine(settingsRepository.lockOnDoubleTap) { base, enabled -> base.copy(lockOnDoubleTap = enabled) }

    private data class HomeContent(
        val base: BaseSettings,
        val apps: List<AppInfo> = emptyList(),
        val loading: Boolean = true,
        val todos: List<TodoItem> = emptyList(),
        val chapters: List<ChapterItem> = emptyList(),
        val pdfs: List<PdfLink> = emptyList()
    )

    val uiState: StateFlow<HomeUiState> = baseState.combine(_apps) { base, apps -> HomeContent(base, apps = apps) }
        .combine(_isLoadingApps) { content, loading -> content.copy(loading = loading) }
        .combine(settingsRepository.todoList) { content, todos -> content.copy(todos = todos) }
        .combine(settingsRepository.chapterList) { content, chapters -> content.copy(chapters = chapters) }
        .combine(settingsRepository.pdfList) { content, pdfs -> content.copy(pdfs = pdfs) }
        .map { content ->
        val base = content.base
        val apps = content.apps
        val visibleApps = if (base.focusActive) apps.filter { it.packageName in base.allowedApps } else apps
        val resolvedShortcuts = base.appShortcutRefs.mapNotNull { ref -> apps.find { it.packageName == ref.packageName && it.activityClassName == ref.activityClassName } }
        val resolvedRecents = base.recentAppRefs.mapNotNull { ref -> apps.find { it.packageName == ref.packageName && it.activityClassName == ref.activityClassName } }
        HomeUiState(
            greeting = buildGreeting(base.name), apps = visibleApps, widgetVisibility = base.visibility, widgetColumns = base.columns, widgetSizes = base.sizes, widgetHeights = base.heights, widgetWidths = base.widths,
            examCountdowns = base.exam.exams.map { exam -> ExamCountdown(exam.id, exam.name, exam.dateMillis?.let { CountdownUtil.daysRemaining(it) }) },
            todoItems = content.todos, chapterItems = content.chapters, pdfLinks = content.pdfs,
            isFocusModeActive = base.focusActive, background = base.background, isLoadingApps = content.loading,
            milestoneTarget = base.milestoneTarget, appShortcuts = resolvedShortcuts, recentApps = resolvedRecents,
            appCategories = base.appCategories, appCategoryTypes = base.appCategoryTypes, lockOnDoubleTap = base.lockOnDoubleTap
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    /** Rotates through a few phrases so the greeting doesn't feel static every day. */
    private fun buildGreeting(name: String): String {
        val phrases = listOf("Welcome back, %s", "Keep grinding, %s!", "One step closer, %s", "Focus mode on, %s")
        val dayOfYear = LocalDate.now().dayOfYear
        return phrases[dayOfYear % phrases.size].format(name)
    }

    fun launchApp(app: AppInfo) {
        appRepository.launchApp(app.packageName, app.activityClassName)
        viewModelScope.launch {
            settingsRepository.recordAppLaunch(AppShortcutRef(app.packageName, app.activityClassName))
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
        settingsRepository.setWidgetHeight(id, (uiState.value.widgetHeights[id] ?: legacyHeight) + deltaDp)
    }

    fun adjustWidgetWidth(id: String, deltaPercent: Int) = viewModelScope.launch {
        settingsRepository.setWidgetWidth(id, (uiState.value.widgetWidths[id] ?: 100) + deltaPercent)
    }

    /** Sensible first-run categorisation; users can freely change every assignment afterwards. */
    private fun suggestedCategory(app: AppInfo): String {
        val identity = "${app.packageName} ${app.label}".lowercase()
        return when {
            listOf("youtube", "netflix", "primevideo", "hotstar", "spotify", "music", "video", "tv").any(identity::contains) -> AppCategory.ENTERTAINMENT.displayName
            listOf("whatsapp", "telegram", "instagram", "facebook", "snapchat", "discord", "messenger", "linkedin", "x.com", "twitter").any(identity::contains) -> AppCategory.SOCIAL.displayName
            listOf("classroom", "meet", "zoom", "notion", "docs", "drive", "khan", "coursera", "unacademy", "byju", "physicswallah", "study", "exam", "learn").any(identity::contains) -> AppCategory.STUDY.displayName
            listOf("game", "games", "supercell", "roblox", "minecraft", "pubg", "freefire").any(identity::contains) -> AppCategory.GAMES.displayName
            else -> AppCategory.OTHER.displayName
        }
    }

    // ---------- Daily to-do ----------
    fun addTodo(text: String) = viewModelScope.launch {
        if (text.isBlank()) return@launch
        val updated = uiState.value.todoItems + TodoItem(id = UUID.randomUUID().toString(), text = text.trim())
        settingsRepository.setTodoList(updated)
    }

    fun toggleTodo(id: String) = viewModelScope.launch {
        val updated = uiState.value.todoItems.map { if (it.id == id) it.copy(isDone = !it.isDone) else it }
        settingsRepository.setTodoList(updated)
    }

    fun deleteTodo(id: String) = viewModelScope.launch {
        settingsRepository.setTodoList(uiState.value.todoItems.filterNot { it.id == id })
    }

    // ---------- Chapter backlog ----------
    fun addChapter(subject: String, chapterName: String, status: ChapterStatus) = viewModelScope.launch {
        if (chapterName.isBlank()) return@launch
        val item = ChapterItem(id = UUID.randomUUID().toString(), subject = subject, chapterName = chapterName.trim(), status = status)
        settingsRepository.setChapterList(uiState.value.chapterItems + item)
    }

    fun deleteChapter(id: String) = viewModelScope.launch {
        settingsRepository.setChapterList(uiState.value.chapterItems.filterNot { it.id == id })
    }

    // ---------- PDF quick-launch ----------
    fun addPdfLink(title: String, uriString: String) = viewModelScope.launch {
        if (title.isBlank() || uriString.isBlank()) return@launch
        val link = PdfLink(id = UUID.randomUUID().toString(), title = title.trim(), uriString = uriString)
        settingsRepository.setPdfList(uiState.value.pdfLinks + link)
    }

    fun deletePdfLink(id: String) = viewModelScope.launch {
        settingsRepository.setPdfList(uiState.value.pdfLinks.filterNot { it.id == id })
    }

    // ---------- Milestone / target ----------
    fun setMilestoneTarget(target: MilestoneTarget) = viewModelScope.launch {
        settingsRepository.setMilestoneTarget(target)
    }

    // ---------- App Shortcuts widget ----------
    fun addAppShortcut(app: AppInfo) = viewModelScope.launch {
        val current = settingsRepository.appShortcuts.first()
        val ref = AppShortcutRef(app.packageName, app.activityClassName)
        if (current.any { it.packageName == ref.packageName && it.activityClassName == ref.activityClassName }) return@launch
        settingsRepository.setAppShortcuts(current + ref)
    }

    fun removeAppShortcut(app: AppInfo) = viewModelScope.launch {
        val current = settingsRepository.appShortcuts.first()
        settingsRepository.setAppShortcuts(
            current.filterNot { it.packageName == app.packageName && it.activityClassName == app.activityClassName }
        )
    }

    fun removeFromRecents(app: AppInfo) = viewModelScope.launch {
        settingsRepository.removeFromRecentApps(AppShortcutRef(app.packageName, app.activityClassName))
    }

    // ---------- App Drawer categories ----------
    fun setAppCategory(app: AppInfo, category: String) = viewModelScope.launch {
        settingsRepository.setAppCategory(app.packageName, category)
    }

    // ---------- Uninstall / app info ----------
    /** Only apps not flagged as system apps can be uninstalled - matches Android's own rule. */
    fun canUninstall(app: AppInfo): Boolean = !app.isSystemApp
    fun uninstallApp(app: AppInfo) = appRepository.uninstallApp(app.packageName)
    fun openAppInfo(app: AppInfo) = appRepository.openAppInfo(app.packageName)
}
