package com.zenith.launcher.ui.home
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.zenith.launcher.data.model.WidgetIds
import com.zenith.launcher.ui.home.components.AddBacklogDialog
import com.zenith.launcher.ui.home.components.AddDeadlineDialog
import com.zenith.launcher.ui.home.components.EditBacklogDialog
import com.zenith.launcher.ui.home.components.EditTodoDialog
import com.zenith.launcher.ui.home.components.AddLibraryDialog
import com.zenith.launcher.ui.home.components.AddTodoDialog
import com.zenith.launcher.ui.home.components.AppDrawerOverlay
import com.zenith.launcher.ui.home.components.GreetingHeader
import com.zenith.launcher.ui.home.components.GridDragDropState
import com.zenith.launcher.ui.home.components.HomeBackground
import com.zenith.launcher.ui.home.components.gridDragToReorder
import com.zenith.launcher.ui.home.components.rememberGridDragDropState
import com.zenith.launcher.ui.home.components.reportColumnBounds
import com.zenith.launcher.util.SystemActionsHelper
import kotlinx.coroutines.launch


/** How many columns the widget grid lays widgets out into - matches the reference design. */
private const val GRID_COLUMN_COUNT = 3

/** Cumulative horizontal drag (px) needed before a right-edge swipe counts as "open the drawer". */
private const val DRAWER_SWIPE_OPEN_THRESHOLD_PX = 60f

/**
 * Width of the invisible left/right edge-swipe strips - deliberately kept equal to the grid Row's
 * own outer horizontal padding (see the Row in [HomeScreen]) so the strips live entirely within
 * that visual margin and never overlap a widget's actual touch area.
 */
private val EDGE_SWIPE_STRIP_WIDTH = 32.dp

/**
 * Only these widgets stay usable while a Pomodoro focus session is running - every other widget
 * is blurred *and* has its touches blocked (see the grid loop in [HomeScreen]), the Settings
 * gear and the App Drawer edge-swipe are disabled, and the grid can't be rearranged. The lock
 * lifts the moment the session is completed, paused, or reset (i.e. whenever the Pomodoro
 * widget itself reports back that it's no longer running).
 */
private val POMODORO_ACCESSIBLE_WIDGETS = setOf(
    WidgetIds.POMODORO,
    WidgetIds.TODO,
    WidgetIds.BACKLOG,
    WidgetIds.LIBRARY
)

/**
 * The launcher's home screen: a live clock + greeting header, a hold-and-drag reorderable
 * 3-column widget grid, and a right-edge swipe (right-to-left) that opens the App Drawer as its
 * own full-screen area (see [AppDrawerOverlay]) - installed apps never live inside this grid.
 */
@Composable
fun HomeScreen(viewModel: HomeViewModel, onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    var showAddTodoDialog by remember { mutableStateOf(false) }
    var editingTodo by remember { mutableStateOf<com.zenith.launcher.data.model.TodoItem?>(null) }
    var showAddBacklogDialog by remember { mutableStateOf(false) }
    var editingBacklog by remember { mutableStateOf<com.zenith.launcher.data.model.BacklogItem?>(null) }
    var showAddDeadlineDialog by remember { mutableStateOf(false) }
    var showAddLibraryDialog by remember { mutableStateOf(false) }
    var isDrawerOpen by remember { mutableStateOf(false) }
    var isPomodoroRunning by remember { mutableStateOf(false) }
    var showFocusEntryPause by remember { mutableStateOf(false) }
    var showFocusExitPause by remember { mutableStateOf(false) }
    var isPomodoroProtectionActive by remember { mutableStateOf(false) }
    var pomodoroPauseSeconds by remember { mutableStateOf(0) }
    var pomodoroPauseMessage by remember { mutableStateOf("") }
    var resizePreviewDelta by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }

    // Protect only deliberate focus/reflection windows, not the whole Pomodoro. This keeps the
    // Android system surface available during ordinary study time while closing the escape hatch
    // during moments when Zenith is asking the student to pause and choose deliberately.
    val systemUiProtected = state.isFocusModeActive || showFocusEntryPause || showFocusExitPause ||
        state.distractionPauseSeconds > 0 || isPomodoroProtectionActive
    val window = (context as? android.app.Activity)?.window
    LaunchedEffect(systemUiProtected, state.attentionProtectionMode, window) {
        window?.let {
            SystemActionsHelper.setAttentionProtection(
                window = it,
                context = context,
                mode = state.attentionProtectionMode,
                protected = systemUiProtected
            )
        }
    }

    val phoneLayout = LocalConfiguration.current.screenWidthDp < 600
    // Phones use one comfortable reading column; tablets keep the existing three-column layout.
    // The saved three-column arrangement is never rewritten merely because a phone is displaying it.
    val visibleColumns = remember(state.widgetColumns, state.phoneWidgetOrder, state.widgetVisibility, phoneLayout) {
        if (phoneLayout) {
            listOf(state.phoneWidgetOrder.filter { isWidgetEnabled(it, state.widgetVisibility) })
        } else {
            state.widgetColumns.map { column ->
                column.filter { id -> isWidgetEnabled(id, state.widgetVisibility) }
            }
        }
    }
    val dragState = rememberGridDragDropState(columns = visibleColumns) { id, toColumn, toIndex ->
        if (phoneLayout) viewModel.movePhoneWidget(id, toIndex)
        else viewModel.moveWidget(id, toColumn, toIndex)
    }

    // A launcher's onResume fires both on a cold "go to Home" and - if this was the last
    // foreground app before the phone locked - the moment the user unlocks again, so replaying
    // a quick fade + scale-in here on every resume covers "widgets loading in after unlock"
    // without needing to hook into ACTION_USER_PRESENT separately.
    var resumeTrigger by remember { mutableStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                resumeTrigger++
                // Package install/uninstall and system app-info screens return Home through this
                // path; refreshing here makes the drawer, shortcuts and icon pack resolve at
                // once instead of retaining a stale process-local list.
                viewModel.loadApps()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val contentAlpha = remember { Animatable(0f) }
    val contentScale = remember { Animatable(0.96f) }
    LaunchedEffect(resumeTrigger) {
        contentAlpha.snapTo(0f)
        contentScale.snapTo(0.96f)
        launch { contentAlpha.animateTo(1f, tween(420)) }
        launch { contentScale.animateTo(1f, tween(420, easing = FastOutSlowInEasing)) }
    }

    // System back closes the app drawer if it's open, otherwise exits widget-rearranging mode if
    // that's active - both take priority over the launcher's normal "swallow back" behaviour.
    BackHandler(enabled = isDrawerOpen) { isDrawerOpen = false }
    BackHandler(enabled = !isDrawerOpen && dragState.editMode) { dragState.exitEditMode() }
    BackHandler(enabled = showFocusEntryPause || showFocusExitPause) { /* The reflection completes automatically. */ }

    Box(modifier = Modifier.fillMaxSize()) {
        HomeBackground(background = state.background)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .graphicsLayer {
                    alpha = contentAlpha.value
                    scaleX = contentScale.value
                    scaleY = contentScale.value
                }
                // Attach this to the full Home surface rather than only the header. Child
                // widgets keep their own gestures; a double tap on any unused Home space locks.
                .then(
                    if (state.lockOnDoubleTap) {
                        Modifier.pointerInput(state.lockOnDoubleTap) {
                            detectTapGestures(onDoubleTap = { SystemActionsHelper.lockScreen(context) })
                        }
                    } else Modifier
                )
        ) {
            GreetingHeader(
                greeting = state.greeting,
                // The Settings gear is one of the things a running Pomodoro session locks out -
                // disabled (not hidden) so it's clear why it's unresponsive, and the header
                // itself is left crisp rather than blurred like the rest of the locked screen.
                onSettingsClick = onOpenSettings,
                settingsEnabled = !isPomodoroRunning,
                overPhotoBackground = state.background.imageUri != null,
                lockOnDoubleTap = state.lockOnDoubleTap
            )

            Box(modifier = Modifier.weight(1f)) {
                if (visibleColumns.all { it.isEmpty() }) {
                    Text(
                        text = "Your Home is ready. Open Settings to enable the widgets you want.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = androidx.compose.ui.graphics.Color.White,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.88f))
                            .clickable(onClick = onOpenSettings)
                            .padding(horizontal = 24.dp, vertical = 18.dp)
                    )
                }
                if (phoneLayout) {
                    if (dragState.editMode) {
                        Text(
                            "Hold and drag a widget to rearrange it. Drag the corner to resize.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.78f),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 2.dp)
                                .zIndex(1f)
                        )
                    }
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .reportColumnBounds(dragState, 0)
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                            .then(
                                if (dragState.editMode) {
                                    Modifier.pointerInput(Unit) { detectTapGestures { dragState.exitEditMode() } }
                                } else Modifier
                            ),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        userScrollEnabled = !dragState.editMode
                    ) {
                        items(visibleColumns.firstOrNull().orEmpty(), key = { it }) { id ->
                            PhoneWidgetItem(
                                id = id, state = state, viewModel = viewModel, dragState = dragState,
                                resizePreviewDelta = resizePreviewDelta,
                                onResizePreview = { delta -> resizePreviewDelta = resizePreviewDelta + (id to delta) },
                                entranceTrigger = resumeTrigger,
                                entranceIndex = visibleColumns.firstOrNull()?.indexOf(id) ?: 0,
                                onResizeEnd = { totalDelta ->
                                    if (totalDelta != 0) viewModel.adjustWidgetHeight(id, totalDelta)
                                    resizePreviewDelta = resizePreviewDelta - id
                                },
                                onShowAddTodo = { showAddTodoDialog = true }, onEditTodo = { editingTodo = it },
                                onShowAddBacklog = { showAddBacklogDialog = true }, onEditBacklog = { editingBacklog = it },
                                onShowAddDeadline = { showAddDeadlineDialog = true }, onShowAddLibrary = { showAddLibraryDialog = true },
                                onFocusToggle = { if (state.isFocusModeActive) showFocusExitPause = true else if (!showFocusEntryPause) showFocusEntryPause = true },
                                onPomodoroRunningChanged = { isPomodoroRunning = it },
                                onPomodoroProtectionChanged = { isPomodoroProtectionActive = it },
                                onPomodoroPauseChanged = { active, seconds, message ->
                                    pomodoroPauseSeconds = if (active) seconds else 0
                                    pomodoroPauseMessage = if (active) message else ""
                                }
                            )
                        }
                    }
                } else Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .then(if (dragState.editMode) Modifier.pointerInput(Unit) { detectTapGestures { dragState.exitEditMode() } } else Modifier),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    for (columnIndex in 0 until if (phoneLayout) 1 else GRID_COLUMN_COUNT) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .reportColumnBounds(dragState, columnIndex),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            visibleColumns.getOrNull(columnIndex)?.forEach { id ->
                                // Explicit key by widget id (not just position) so a widget's own
                                // local state - e.g. a running Pomodoro timer - stays attached to
                                // that widget as it moves, instead of Compose reattaching state to
                                // whatever now sits in the old slot.
                                key(id) {
                                    val isDragging = dragState.isDragging(id)
                                    val isLockedByPomodoro = isPomodoroRunning && id !in POMODORO_ACCESSIBLE_WIDGETS
                                    val baseHeight = state.widgetHeights[id] ?: WidgetIds.DEFAULT_HEIGHTS[id] ?: state.widgetSizes[id]?.minHeightDp ?: 160
                                    val minimumHeight = if (id == WidgetIds.DEADLINES) 188 else 88
                                    val displayedHeight = (baseHeight + (resizePreviewDelta[id] ?: 0)).coerceIn(minimumHeight, 600)
                                    WidgetEntrance(
                                        entranceTrigger = resumeTrigger,
                                        entranceIndex = columnIndex * 3 + (visibleColumns.getOrNull(columnIndex)?.indexOf(id) ?: 0)
                                    ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            // The real widget is only ever hidden, never moved by
                                            // hand, while its ghost (below) does the floating -
                                            // see GridDragDropState's class doc for why.
                                            .alpha(if (isDragging) 0f else 1f)
                                            // The whole grid is frozen in place for the duration of
                                            // a Pomodoro session - not just the locked-out widgets -
                                            // so the layout can't shift under the three that remain
                                            // usable.
                                            .then(
                                                if (isPomodoroRunning) Modifier else Modifier.gridDragToReorder(dragState, id)
                                            )
                                            .then(if (isLockedByPomodoro) Modifier.blur(10.dp) else Modifier)
                                            .height(displayedHeight.dp)
                                    ) {
                                        WidgetForId(
                                            id = id,
                                            state = state,
                                            viewModel = viewModel,
                                            onShowAddTodo = { showAddTodoDialog = true },
                                            onEditTodo = { editingTodo = it },
                                            onShowAddBacklog = { showAddBacklogDialog = true },
                                            onEditBacklog = { editingBacklog = it },
                                            onShowAddDeadline = { showAddDeadlineDialog = true },
                                            onShowAddLibrary = { showAddLibraryDialog = true },
                                            onFocusToggle = {
                                                if (state.isFocusModeActive) {
                                                    showFocusExitPause = true
                                                } else if (!showFocusEntryPause) {
                                                    showFocusEntryPause = true
                                                }
                                            },
                                            onPomodoroRunningChanged = { isPomodoroRunning = it },
                                            onPomodoroProtectionChanged = { isPomodoroProtectionActive = it },
                                            onPomodoroPauseChanged = { active, seconds, message ->
                                                pomodoroPauseSeconds = if (active) seconds else 0
                                                pomodoroPauseMessage = if (active) message else ""
                                            }
                                        )
                                        if (isLockedByPomodoro) {
                                            // Keep locked widgets recognizable but visually subordinate:
                                            // the stronger blur makes the active Pomodoro surface feel like
                                            // the only thing that matters, while the soft veil preserves
                                            // enough context to remind the student that the rest still exists.
                                            Box(
                                                modifier = Modifier
                                                    .matchParentSize()
                                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.12f))
                                                    .pointerInput(id) { detectTapGestures { } }
                                            )
                                        }
                                        if (dragState.editMode) {
                                            WidgetResizeGrip(
                                                onHeightPreview = { delta ->
                                                    resizePreviewDelta = resizePreviewDelta + (id to delta)
                                                },
                                                onHeightChangeEnd = { totalDelta ->
                                                    if (totalDelta != 0) viewModel.adjustWidgetHeight(id, totalDelta)
                                                    resizePreviewDelta = resizePreviewDelta - id
                                                },
                                                modifier = Modifier.align(Alignment.BottomEnd)
                                            )
                                        }
                                    }
                                    }
                                }
                            }
                        }
                    }
                }

                DragGhostOverlay(dragState = dragState) { id ->
                    WidgetForId(
                        id = id,
                        state = state,
                        viewModel = viewModel,
                        onShowAddTodo = {},
                        onEditTodo = {},
                        onShowAddBacklog = {},
                        onEditBacklog = {},
                        onShowAddDeadline = {},
                        onShowAddLibrary = {},
                        onFocusToggle = {},
                        onPomodoroRunningChanged = {}
                    )
                }

                EditModeDonePill(
                    visible = dragState.editMode,
                    onDone = { dragState.exitEditMode() },
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp)
                )
            }
        }

        // Invisible strip along the right edge of the screen: swiping right-to-left starting
        // from here opens the App Drawer, mirroring how edge swipes work elsewhere on Android
        // without hijacking horizontal gestures used by widgets in the middle of the screen.
        // Sized to sit exactly within the grid Row's own outer padding (see the Row above) so it
        // never overlaps a widget's actual touch area - previously it was wider than that margin
        // and could steal the very first pixels of a drag starting from the rightmost column.
        // It's also fully disabled while rearranging, so it can never compete with a drag at all -
        // and disabled for the duration of a running Pomodoro session, since the App Drawer isn't
        // one of the widgets a session leaves accessible.
        if (!isDrawerOpen && !dragState.editMode && !isPomodoroRunning) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(EDGE_SWIPE_STRIP_WIDTH)
                    // Reserve this narrow strip for Zenith's drawer gesture rather than letting
                    // Android's edge-back gesture consume the first part of the swipe.
                    .systemGestureExclusion()
                    .pointerInput(Unit) {
                        var accumulated = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { accumulated = 0f },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                accumulated += dragAmount
                                if (accumulated < -DRAWER_SWIPE_OPEN_THRESHOLD_PX) isDrawerOpen = true
                            }
                        )
                    }
            )
        }

        AnimatedVisibility(
            visible = isDrawerOpen,
            enter = slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth }),
            exit = slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth }),
            modifier = Modifier.fillMaxSize()
        ) {
            AppDrawerOverlay(
                apps = state.apps,
                categories = state.appCategories,
                categoryTypes = state.appCategoryTypes,
                background = state.background,
                onLaunch = { app -> viewModel.launchApp(app); isDrawerOpen = false },
                onDismiss = { isDrawerOpen = false },
                onUninstall = viewModel::uninstallApp,
                canUninstall = viewModel::canUninstall,
                onOpenAppInfo = viewModel::openAppInfo,
                onSetCategory = viewModel::setAppCategory,
                onPinShortcut = viewModel::addAppShortcut,
                focusAllowedPackages = state.focusAllowedApps,
                distractionPackages = state.distractionApps,
                onToggleFocusAllowed = viewModel::toggleFocusAllowedApp,
                onToggleDistraction = viewModel::toggleDistractionApp
            )
        }

        if (showFocusEntryPause) {
            FocusModeEntryPause(onFinished = {
                showFocusEntryPause = false
                viewModel.toggleFocusMode()
            })
        }

        if (showFocusExitPause) {
            FocusModeExitPause(onFinished = {
                showFocusExitPause = false
                viewModel.toggleFocusMode()
            })
        }

        if (pomodoroPauseSeconds > 0) {
            PomodoroReflectionPause(
                secondsLeft = pomodoroPauseSeconds,
                message = pomodoroPauseMessage
            )
        }

        state.distractionPauseApp?.let {
            DistractionLaunchPause(
                secondsLeft = state.distractionPauseSeconds,
                message = state.distractionPauseMessage
            )
        }
    }

    if (showAddDeadlineDialog) {
        AddDeadlineDialog(
            onDismiss = { showAddDeadlineDialog = false },
            onConfirm = { name, date -> viewModel.addDeadline(name, date); showAddDeadlineDialog = false }
        )
    }
    if (editingTodo != null) {
        EditTodoDialog(
            item = editingTodo!!,
            onDismiss = { editingTodo = null },
            onConfirm = { item -> viewModel.updateTodo(item); editingTodo = null }
        )
    }
    if (editingBacklog != null) {
        EditBacklogDialog(
            item = editingBacklog!!,
            onDismiss = { editingBacklog = null },
            onConfirm = { item -> viewModel.updateBacklogItem(item); editingBacklog = null }
        )
    }
    if (showAddTodoDialog) {
        AddTodoDialog(
            onDismiss = { showAddTodoDialog = false },
            onConfirm = { text -> viewModel.addTodo(text); showAddTodoDialog = false }
        )
    }
    if (showAddBacklogDialog) {
        AddBacklogDialog(
            onDismiss = { showAddBacklogDialog = false },
            onConfirm = { subject, itemName, urgency ->
                viewModel.addBacklogItem(subject, itemName, urgency)
                showAddBacklogDialog = false
            }
        )
    }
    if (showAddLibraryDialog) {
        AddLibraryDialog(
            onDismiss = { showAddLibraryDialog = false },
            onConfirm = { title, uri -> viewModel.addLibraryLink(title, uri); showAddLibraryDialog = false }
        )
    }
}
