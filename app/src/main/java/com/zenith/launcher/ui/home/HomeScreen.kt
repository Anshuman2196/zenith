package com.zenith.launcher.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.zenith.launcher.data.model.WidgetIds
import com.zenith.launcher.ui.home.components.AddChapterDialog
import com.zenith.launcher.ui.home.components.AddPdfDialog
import com.zenith.launcher.ui.home.components.AddTodoDialog
import com.zenith.launcher.ui.home.components.AppDrawerOverlay
import com.zenith.launcher.ui.home.components.AppShortcutsWidget
import com.zenith.launcher.ui.home.components.ChapterBacklogWidget
import com.zenith.launcher.ui.home.components.CountdownWidget
import com.zenith.launcher.ui.home.components.FocusModeToggle
import com.zenith.launcher.ui.home.components.GreetingHeader
import com.zenith.launcher.ui.home.components.GridDragDropState
import com.zenith.launcher.ui.home.components.HomeBackground
import com.zenith.launcher.ui.home.components.MilestoneWidget
import com.zenith.launcher.ui.home.components.PdfLauncherWidget
import com.zenith.launcher.ui.home.components.PomodoroWidget
import com.zenith.launcher.ui.home.components.SystemStatusWidget
import com.zenith.launcher.ui.home.components.TodoWidget
import com.zenith.launcher.ui.home.components.gridDragToReorder
import com.zenith.launcher.ui.home.components.rememberGridDragDropState
import com.zenith.launcher.ui.home.components.reportColumnBounds
import com.zenith.launcher.util.SystemActionsHelper
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

/** How many columns the widget grid lays widgets out into - matches the reference design. */
private const val GRID_COLUMN_COUNT = 3

/** Cumulative horizontal drag (px) needed before a right-edge swipe counts as "open the drawer". */
private const val DRAWER_SWIPE_OPEN_THRESHOLD_PX = 60f

/**
 * Width of the invisible left/right edge-swipe strips - deliberately kept equal to the grid Row's
 * own outer horizontal padding (see the Row in [HomeScreen]) so the strips live entirely within
 * that visual margin and never overlap a widget's actual touch area.
 */
private val EDGE_SWIPE_STRIP_WIDTH = 12.dp

/** These productive widgets stay readable while a Pomodoro is in progress. */
private val POMODORO_BLUR_EXEMPT_WIDGETS = setOf(
    WidgetIds.POMODORO,
    WidgetIds.TODO,
    WidgetIds.CHAPTER_BACKLOG,
    WidgetIds.PDF_LAUNCHER
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
    var showAddChapterDialog by remember { mutableStateOf(false) }
    var showAddPdfDialog by remember { mutableStateOf(false) }
    var isDrawerOpen by remember { mutableStateOf(false) }
    var isPomodoroRunning by remember { mutableStateOf(false) }
    var showFocusExitPause by remember { mutableStateOf(false) }

    val visibleColumns = remember(state.widgetColumns, state.widgetVisibility) {
        state.widgetColumns.map { column ->
            column.filter { id -> isWidgetEnabled(id, state.widgetVisibility) }
        }
    }
    val dragState = rememberGridDragDropState(columns = visibleColumns) { id, toColumn, toIndex ->
        viewModel.moveWidget(id, toColumn, toIndex)
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
    BackHandler(enabled = showFocusExitPause) { /* The reflection completes automatically. */ }

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
                onSettingsClick = onOpenSettings,
                overPhotoBackground = state.background.imageUri != null,
                lockOnDoubleTap = state.lockOnDoubleTap,
                modifier = Modifier.blur(if (isPomodoroRunning) 10.dp else 0.dp)
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
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        // While rearranging, a tap on empty grid space (i.e. not consumed by any
                        // widget's own drag/click handling) finishes editing - the same as
                        // tapping empty space on the stock Android home screen.
                        .then(
                            if (dragState.editMode) {
                                Modifier.pointerInput(Unit) { detectTapGestures { dragState.exitEditMode() } }
                            } else {
                                Modifier
                            }
                        ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    for (columnIndex in 0 until GRID_COLUMN_COUNT) {
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
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth((state.widgetWidths[id] ?: 100) / 100f)
                                            // The real widget is only ever hidden, never moved by
                                            // hand, while its ghost (below) does the floating -
                                            // see GridDragDropState's class doc for why.
                                            .alpha(if (isDragging) 0f else 1f)
                                            .gridDragToReorder(dragState, id)
                                            .then(
                                                if (isPomodoroRunning && id !in POMODORO_BLUR_EXEMPT_WIDGETS) {
                                                    Modifier.blur(10.dp)
                                                } else Modifier
                                            )
                                            .height((state.widgetHeights[id] ?: state.widgetSizes[id]?.minHeightDp ?: 160).coerceIn(96, 600).dp)
                                    ) {
                                        WidgetForId(
                                            id = id,
                                            state = state,
                                            viewModel = viewModel,
                                            onShowAddTodo = { showAddTodoDialog = true },
                                            onShowAddChapter = { showAddChapterDialog = true },
                                            onShowAddPdf = { showAddPdfDialog = true },
                                            onFocusToggle = {
                                                if (state.isFocusModeActive) showFocusExitPause = true
                                                else viewModel.toggleFocusMode()
                                            },
                                            onPomodoroRunningChanged = { isPomodoroRunning = it }
                                        )
                                        if (dragState.editMode) {
                                            WidgetResizeGrip(
                                                onHeightDelta = { viewModel.adjustWidgetHeight(id, it) },
                                                onWidthDelta = { viewModel.adjustWidgetWidth(id, it) },
                                                modifier = Modifier.align(Alignment.BottomEnd)
                                            )
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
                        onShowAddChapter = {},
                        onShowAddPdf = {},
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
        // It's also fully disabled while rearranging, so it can never compete with a drag at all.
        if (!isDrawerOpen && !dragState.editMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(EDGE_SWIPE_STRIP_WIDTH)
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
                onPinShortcut = viewModel::addAppShortcut
            )
        }

        if (showFocusExitPause) {
            FocusModeExitPause(onFinished = {
                showFocusExitPause = false
                viewModel.toggleFocusMode()
            })
        }
    }

    if (showAddTodoDialog) {
        AddTodoDialog(
            onDismiss = { showAddTodoDialog = false },
            onConfirm = { text -> viewModel.addTodo(text); showAddTodoDialog = false }
        )
    }
    if (showAddChapterDialog) {
        AddChapterDialog(
            onDismiss = { showAddChapterDialog = false },
            onConfirm = { subject, chapter, status ->
                viewModel.addChapter(subject, chapter, status)
                showAddChapterDialog = false
            }
        )
    }
    if (showAddPdfDialog) {
        AddPdfDialog(
            onDismiss = { showAddPdfDialog = false },
            onConfirm = { title, uri -> viewModel.addPdfLink(title, uri); showAddPdfDialog = false }
        )
    }
}

/** Drag the diagonal corner grip to resize a widget; no preset-size buttons are needed. */
@Composable
private fun WidgetResizeGrip(
    onHeightDelta: (Int) -> Unit,
    onWidthDelta: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(28.dp)
            .clip(RoundedCornerShape(topStart = 10.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f))
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    if (kotlin.math.abs(dragAmount.y) > 8f) onHeightDelta(if (dragAmount.y > 0) 24 else -24)
                    if (kotlin.math.abs(dragAmount.x) > 8f) onWidthDelta(if (dragAmount.x > 0) 10 else -10)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text("↘", color = Color.White, style = MaterialTheme.typography.labelLarge)
    }
}

/**
 * A deliberately non-interactive pause before ending Focus Mode. It is not a confirmation:
 * after a short breathing interval the mode is switched off automatically.
 */
@Composable
private fun FocusModeExitPause(onFinished: () -> Unit) {
    var secondsLeft by remember { mutableStateOf(18) }
    val messages = listOf(
        "Are you sure you want to do this?",
        "Was your session good enough?",
        "Take one slow breath before you leave focus.",
        "Notice what you completed — then return with intention."
    )
    val breathing = rememberInfiniteTransition(label = "focusExitBreath")
    val ringScale by breathing.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(3500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "focusExitBreathScale"
    )
    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            delay(1000)
            secondsLeft--
        }
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.78f))
            // Consume taps so this stays a reflection rather than an accidental confirmation.
            .pointerInput(Unit) { detectTapGestures { } },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(210.dp)
                .graphicsLayer { scaleX = ringScale; scaleY = ringScale }
                .clip(RoundedCornerShape(105.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
        )
        Column(
            modifier = Modifier.padding(horizontal = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Pause before leaving Focus Mode", style = MaterialTheme.typography.titleLarge, color = Color.White)
            Text(
                messages[(18 - secondsLeft) / 5 % messages.size],
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Text(
                "Exhale slowly · continuing in $secondsLeft s",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "Focus Mode will turn off automatically. No response is needed.",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

/**
 * The single floating "ghost" copy of whichever widget is currently being dragged (see
 * [GridDragDropState]'s class doc for why this exists). Positioned directly from
 * [GridDragDropState.dragPointerRoot] with no dependency on the grid's own layout, so it can
 * never jitter from a layout-timing race the way an in-place offset could.
 */
@Composable
private fun DragGhostOverlay(dragState: GridDragDropState, content: @Composable (id: String) -> Unit) {
    val id = dragState.draggingId ?: return
    val size = dragState.draggingItemSize ?: return
    val density = LocalDensity.current

    val widthDp = with(density) { size.width.toDp() }
    val heightDp = with(density) { size.height.toDp() }
    val leftPx = dragState.dragPointerRoot.x - size.width / 2f
    val topPx = dragState.dragPointerRoot.y - size.height / 2f

    Box(
        modifier = Modifier
            .offset { IntOffset(leftPx.roundToInt(), topPx.roundToInt()) }
            .width(widthDp)
            .height(heightDp)
            .graphicsLayer {
                scaleX = 1.05f
                scaleY = 1.05f
                shadowElevation = 24f
            }
            .zIndex(2f)
            .alpha(0.96f)
    ) {
        content(id)
    }
}

/** Small floating pill shown at the bottom of the grid while rearranging - tap to finish. */
@Composable
private fun EditModeDonePill(visible: Boolean, onDone: () -> Unit, modifier: Modifier = Modifier) {
    AnimatedVisibility(visible = visible, modifier = modifier) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.primary)
                .clickable(onClick = onDone)
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Done rearranging",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

/** Whether widget [id] is currently toggled on in Settings > Widgets. */
private fun isWidgetEnabled(id: String, visibility: com.zenith.launcher.data.model.WidgetVisibility): Boolean = when (id) {
    WidgetIds.COUNTDOWN -> visibility.countdownEnabled
    WidgetIds.FOCUS_MODE -> visibility.focusModeEnabled
    WidgetIds.POMODORO -> visibility.pomodoroEnabled
    WidgetIds.TODO -> visibility.todoEnabled
    WidgetIds.CHAPTER_BACKLOG -> visibility.chapterBacklogEnabled
    WidgetIds.PDF_LAUNCHER -> visibility.pdfLauncherEnabled
    WidgetIds.MILESTONE -> visibility.milestoneEnabled
    WidgetIds.APP_SHORTCUTS -> visibility.appShortcutsEnabled
    WidgetIds.SYSTEM_STATUS -> visibility.systemStatusEnabled
    else -> false
}

@Composable
private fun WidgetForId(
    id: String,
    state: HomeUiState,
    viewModel: HomeViewModel,
    onShowAddTodo: () -> Unit,
    onShowAddChapter: () -> Unit,
    onShowAddPdf: () -> Unit,
    onFocusToggle: () -> Unit = {},
    onPomodoroRunningChanged: (Boolean) -> Unit = {}
) {
    when (id) {
        WidgetIds.COUNTDOWN -> CountdownWidget(state.examCountdowns)
        WidgetIds.FOCUS_MODE -> FocusModeToggle(state.isFocusModeActive, onFocusToggle)
        WidgetIds.POMODORO -> PomodoroWidget(onPomodoroRunningChanged)
        WidgetIds.TODO -> TodoWidget(
            items = state.todoItems,
            onAddClick = onShowAddTodo,
            onToggle = viewModel::toggleTodo,
            onDelete = viewModel::deleteTodo
        )
        WidgetIds.CHAPTER_BACKLOG -> ChapterBacklogWidget(
            items = state.chapterItems,
            onAddClick = onShowAddChapter,
            onDelete = viewModel::deleteChapter
        )
        WidgetIds.PDF_LAUNCHER -> PdfLauncherWidget(
            links = state.pdfLinks,
            onAddClick = onShowAddPdf,
            onDelete = viewModel::deletePdfLink
        )
        WidgetIds.MILESTONE -> MilestoneWidget(
            target = state.milestoneTarget,
            onChange = viewModel::setMilestoneTarget
        )
        WidgetIds.APP_SHORTCUTS -> AppShortcutsWidget(
            pinnedApps = state.appShortcuts,
            allApps = state.apps,
            onLaunch = viewModel::launchApp,
            onPin = viewModel::addAppShortcut,
            onUnpin = viewModel::removeAppShortcut
        )
        WidgetIds.SYSTEM_STATUS -> SystemStatusWidget()
    }
}
