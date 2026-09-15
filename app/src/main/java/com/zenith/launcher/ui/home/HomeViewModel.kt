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
import com.zenith.launcher.data.repository.AppRepository
import com.zenith.launcher.data.repository.SettingsRepository
import com.zenith.launcher.util.CountdownUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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
    val appCategories: Map<String, AppCategory> = emptyMap(),
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
        loadApps()
    }

    /** Reloads installed apps, re-skinned with whichever icon pack is currently selected. */
    fun loadApps() {
        viewModelScope.launch {
            _isLoadingApps.value = true
            val iconPack = settingsRepository.iconPackPackage.first()
            _apps.value = appRepository.getInstalledApps(iconPack)
            _isLoadingApps.value = false
        }
    }

    private data class BaseSettings(
        val name: String,
        val exam: ExamSettings,
        val visibility: WidgetVisibility,
        val columns: List<List<String>>,
        val focusActive: Boolean,
        val allowedApps: Set<String>,
        val background: BackgroundSettings,
        val milestoneTarget: MilestoneTarget,
        val appShortcutRefs: List<AppShortcutRef>,
        val recentAppRefs: List<AppShortcutRef>,
        val appCategories: Map<String, AppCategory>,
        val lockOnDoubleTap: Boolean
    )

    private val baseState = combine(
        settingsRepository.profileName,
        settingsRepository.examSettings,
        settingsRepository.widgetVisibility,
        settingsRepository.widgetColumns,
        settingsRepository.focusModeActive,
        settingsRepository.focusAllowedApps,
        settingsRepository.backgroundSettings,
        settingsRepository.milestoneTarget,
        settingsRepository.appShortcuts,
        settingsRepository.recentApps,
        settingsRepository.appCategories,
        settingsRepository.lockOnDoubleTap
    ) { array ->
        BaseSettings(
            name = array[0] as String,
            exam = array[1] as ExamSettings,
            visibility = array[2] as WidgetVisibility,
            // Unchecked cast (as with every other field pulled out of this combine() array) -
            // safe because settingsRepository.widgetColumns is the only Flow<List<List<String>>>
            // fed into this combine call, always in this position.
            columns = array[3] as List<List<String>>,
            focusActive = array[4] as Boolean,
            allowedApps = array[5] as Set<String>,
            background = array[6] as BackgroundSettings,
            milestoneTarget = array[7] as MilestoneTarget,
            @Suppress("UNCHECKED_CAST") appShortcutRefs = array[8] as List<AppShortcutRef>,
            @Suppress("UNCHECKED_CAST") recentAppRefs = array[9] as List<AppShortcutRef>,
            @Suppress("UNCHECKED_CAST") appCategories = array[10] as Map<String, AppCategory>,
            lockOnDoubleTap = array[11] as Boolean
        )
    }

    val uiState: StateFlow<HomeUiState> = combine(
        baseState,
        _apps,
        _isLoadingApps,
        settingsRepository.todoList,
        settingsRepository.chapterList,
        settingsRepository.pdfList
    ) { array ->
        val base = array[0] as BaseSettings
        val apps = array[1] as List<AppInfo>
        val loading = array[2] as Boolean
        val todos = array[3] as List<TodoItem>
        val chapters = array[4] as List<ChapterItem>
        val pdfs = array[5] as List<PdfLink>

        // Focus Mode is an ALLOW-list: when active, only explicitly-allowed apps show up.
        val visibleApps = if (base.focusActive) apps.filter { it.packageName in base.allowedApps } else apps

        // Resolved live (not persisted) so a shortcut always shows the current icon-pack skin
        // and label, and silently drops off if the app's since been uninstalled.
        val resolvedShortcuts = base.appShortcutRefs.mapNotNull { ref ->
            apps.find { it.packageName == ref.packageName && it.activityClassName == ref.activityClassName }
        }
        val resolvedRecents = base.recentAppRefs.mapNotNull { ref ->
            apps.find { it.packageName == ref.packageName && it.activityClassName == ref.activityClassName }
        }

        HomeUiState(
            greeting = buildGreeting(base.name),
            apps = visibleApps,
            widgetVisibility = base.visibility,
            widgetColumns = base.columns,
            examCountdowns = base.exam.exams.map { exam ->
                ExamCountdown(id = exam.id, name = exam.name, daysLeft = exam.dateMillis?.let { CountdownUtil.daysRemaining(it) })
            },
            todoItems = todos,
            chapterItems = chapters,
            pdfLinks = pdfs,
            isFocusModeActive = base.focusActive,
            background = base.background,
            isLoadingApps = loading,
            milestoneTarget = base.milestoneTarget,
            appShortcuts = resolvedShortcuts,
            recentApps = resolvedRecents,
            appCategories = base.appCategories,
            lockOnDoubleTap = base.lockOnDoubleTap
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
    fun setAppCategory(app: AppInfo, category: AppCategory) = viewModelScope.launch {
        settingsRepository.setAppCategory(app.packageName, category)
    }

    // ---------- Uninstall / app info ----------
    /** Only apps not flagged as system apps can be uninstalled - matches Android's own rule. */
    fun canUninstall(app: AppInfo): Boolean = !app.isSystemApp
    fun uninstallApp(app: AppInfo) = appRepository.uninstallApp(app.packageName)
    fun openAppInfo(app: AppInfo) = appRepository.openAppInfo(app.packageName)
}
