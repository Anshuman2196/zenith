package com.zenith.launcher.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenith.launcher.data.model.AppInfo
import com.zenith.launcher.data.model.ChapterItem
import com.zenith.launcher.data.model.ChapterStatus
import com.zenith.launcher.data.model.ExamSettings
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
    val jeeMainDaysLeft: Long? = null,
    val jeeAdvancedDaysLeft: Long? = null,
    val todoItems: List<TodoItem> = emptyList(),
    val chapterItems: List<ChapterItem> = emptyList(),
    val pdfLinks: List<PdfLink> = emptyList(),
    val isFocusModeActive: Boolean = false,
    val isLoadingApps: Boolean = true
)

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
        val focusActive: Boolean,
        val blockedApps: Set<String>
    )

    private val baseState = combine(
        settingsRepository.profileName,
        settingsRepository.examSettings,
        settingsRepository.widgetVisibility,
        settingsRepository.focusModeActive,
        settingsRepository.focusBlockedApps
    ) { name, exam, visibility, focusActive, blockedApps ->
        BaseSettings(name, exam, visibility, focusActive, blockedApps)
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

        val visibleApps = if (base.focusActive) apps.filterNot { it.packageName in base.blockedApps } else apps

        HomeUiState(
            greeting = buildGreeting(base.name),
            apps = visibleApps,
            widgetVisibility = base.visibility,
            jeeMainDaysLeft = base.exam.jeeMainDateMillis?.let { CountdownUtil.daysRemaining(it) },
            jeeAdvancedDaysLeft = base.exam.jeeAdvancedDateMillis?.let { CountdownUtil.daysRemaining(it) },
            todoItems = todos,
            chapterItems = chapters,
            pdfLinks = pdfs,
            isFocusModeActive = base.focusActive,
            isLoadingApps = loading
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    /** Rotates through a few phrases so the greeting doesn't feel static every day. */
    private fun buildGreeting(name: String): String {
        val phrases = listOf("Welcome back, %s", "Keep grinding, %s!", "One step closer, %s", "Focus mode on, %s")
        val dayOfYear = LocalDate.now().dayOfYear
        return phrases[dayOfYear % phrases.size].format(name)
    }

    fun launchApp(app: AppInfo) = appRepository.launchApp(app.packageName, app.activityClassName)

    // ---------- Focus mode ----------
    fun toggleFocusMode() = viewModelScope.launch {
        settingsRepository.setFocusModeActive(!uiState.value.isFocusModeActive)
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
}
