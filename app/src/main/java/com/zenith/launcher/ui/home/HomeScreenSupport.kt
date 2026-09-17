package com.zenith.launcher.ui.home
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.zenith.launcher.data.model.WidgetIds
import com.zenith.launcher.ui.home.components.AppShortcutsWidget
import com.zenith.launcher.ui.home.components.BacklogWidget
import com.zenith.launcher.ui.home.components.DeadlinesWidget
import com.zenith.launcher.ui.home.components.FocusModeToggle
import com.zenith.launcher.ui.home.components.GridDragDropState
import com.zenith.launcher.ui.home.components.TargetsWidget
import com.zenith.launcher.ui.home.components.LibraryWidget
import com.zenith.launcher.ui.home.components.PomodoroWidget
import com.zenith.launcher.ui.home.components.SystemStatusWidget
import com.zenith.launcher.ui.home.components.TodoWidget
import com.zenith.launcher.ui.home.components.gridDragToReorder
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


/** How many columns the widget grid lays widgets out into - matches the reference design. */

@Composable
internal fun PhoneWidgetItem(
    id: String, state: HomeUiState, viewModel: HomeViewModel, dragState: GridDragDropState,
    entranceTrigger: Int, entranceIndex: Int,
    resizePreviewDelta: Map<String, Int>, onResizePreview: (Int) -> Unit, onResizeEnd: (Int) -> Unit,
    onShowAddTodo: () -> Unit, onEditTodo: (com.zenith.launcher.data.model.TodoItem) -> Unit,
    onShowAddBacklog: () -> Unit, onEditBacklog: (com.zenith.launcher.data.model.BacklogItem) -> Unit,
    onShowAddDeadline: () -> Unit, onShowAddLibrary: () -> Unit, onFocusToggle: () -> Unit,
    onPomodoroRunningChanged: (Boolean) -> Unit, onPomodoroProtectionChanged: (Boolean) -> Unit,
    onPomodoroPauseChanged: (Boolean, Int, String) -> Unit
) {
    val baseHeight = state.widgetHeights[id] ?: WidgetIds.DEFAULT_HEIGHTS[id] ?: 160
    val minimumHeight = if (id == WidgetIds.DEADLINES) 188 else 96
    val displayedHeight = (baseHeight + (resizePreviewDelta[id] ?: 0)).coerceIn(minimumHeight, 600)
    val isDragging = dragState.isDragging(id)
    WidgetEntrance(entranceTrigger = entranceTrigger, entranceIndex = entranceIndex) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(displayedHeight.dp)
                .alpha(if (isDragging) 0f else 1f)
                .gridDragToReorder(dragState, id)
        ) {
            WidgetForId(
                id, state, viewModel, onShowAddTodo, onEditTodo, onShowAddBacklog, onEditBacklog,
                onShowAddDeadline, onShowAddLibrary, onFocusToggle, onPomodoroRunningChanged,
                onPomodoroProtectionChanged, onPomodoroPauseChanged
            )
            if (dragState.editMode) {
                WidgetResizeGrip(onHeightPreview = onResizePreview, onHeightChangeEnd = onResizeEnd, modifier = Modifier.align(Alignment.BottomEnd))
            }
        }
    }
}

/**
 * Reveals each widget independently after the launcher becomes visible. The small stagger keeps
 * unlocks feeling intentional instead of making the entire home grid appear in one frame.
 */
@Composable
internal fun WidgetEntrance(
    entranceTrigger: Int,
    entranceIndex: Int,
    content: @Composable () -> Unit
) {
    val alpha = remember { Animatable(0f) }
    val translationY = remember { Animatable(26f) }
    val scale = remember { Animatable(0.97f) }

    LaunchedEffect(entranceTrigger) {
        alpha.snapTo(0f)
        translationY.snapTo(26f)
        scale.snapTo(0.97f)
        delay((entranceIndex.coerceAtMost(7) * 65L))
        launch { alpha.animateTo(1f, tween(320)) }
        launch { translationY.animateTo(0f, tween(420, easing = FastOutSlowInEasing)) }
        launch { scale.animateTo(1f, tween(420, easing = FastOutSlowInEasing)) }
    }

    Box(
        modifier = Modifier.graphicsLayer(
            alpha = alpha.value,
            translationY = translationY.value,
            scaleX = scale.value,
            scaleY = scale.value
        )
    ) {
        content()
    }
}

/** Drag the diagonal corner grip to resize a widget; no preset-size buttons are needed. */
@Composable
internal fun WidgetResizeGrip(
    onHeightPreview: (Int) -> Unit,
    onHeightChangeEnd: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(RoundedCornerShape(topStart = 14.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.88f))
            .pointerInput(Unit) {
                var accumulatedY = 0f
                detectDragGestures(
                    onDragStart = { accumulatedY = 0f },
                    onDragEnd = {
                        val totalDp = with(density) { (accumulatedY / density.density).toInt() }
                        val committedDp = (totalDp / 24) * 24
                        onHeightChangeEnd(committedDp)
                        accumulatedY = 0f
                    },
                    onDragCancel = {
                        onHeightChangeEnd(0)
                        accumulatedY = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        accumulatedY += dragAmount.y
                        val previewDp = with(density) { (accumulatedY / density.density).toInt() }
                        onHeightPreview(previewDp)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text("↕", color = Color.White, style = MaterialTheme.typography.titleMedium)
    }
}

/** A short, non-interactive reflection shown when Zenith intercepts a distraction launch. */
@Composable
internal fun DistractionLaunchPause(secondsLeft: Int, message: String) {
    ReflectionPauseSurface(title = "Distractions", secondsLeft = secondsLeft, message = message, ringSize = 190.dp)
}

/** The same full-screen reflection treatment used by the other deliberate pause moments. */
@Composable
internal fun PomodoroReflectionPause(secondsLeft: Int, message: String) {
    ReflectionPauseSurface(title = "Pomodoro", secondsLeft = secondsLeft, message = message, ringSize = 210.dp)
}

@Composable
internal fun ReflectionPauseSurface(
    title: String,
    secondsLeft: Int,
    message: String,
    ringSize: Dp
) {
    val breathing = rememberInfiniteTransition(label = "reflectionPauseBreath-$title")
    val ringScale by breathing.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.10f,
        animationSpec = infiniteRepeatable(
            tween(2800, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "reflectionPauseBreathScale-$title"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.82f))
            .pointerInput(Unit) { detectTapGestures { } },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(ringSize)
                .graphicsLayer { scaleX = ringScale; scaleY = ringScale }
                .clip(RoundedCornerShape(ringSize / 2))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
        )
        Column(
            modifier = Modifier.padding(horizontal = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = Color.White)
            Text(
                message,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Text(
                secondsLeft.coerceAtLeast(0).toString(),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * A deliberately non-interactive pause before entering Focus Mode. It is not a confirmation:
 * after five seconds the mode starts automatically, giving the user a small moment to notice
 * what they are choosing to give their attention to.
 */
@Composable
internal fun FocusModeEntryPause(onFinished: () -> Unit) {
    var secondsLeft by remember { mutableStateOf(8) }
    val message = remember { ZenithCopy.focusModeEntry.random() }
    val breathing = rememberInfiniteTransition(label = "focusEntryBreath")
    val ringScale by breathing.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            tween(3600, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "focusEntryBreathScale"
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
                .size(200.dp)
                .graphicsLayer { scaleX = ringScale; scaleY = ringScale }
                .clip(RoundedCornerShape(100.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
        )
        Column(
            modifier = Modifier.padding(horizontal = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("A moment before focus", style = MaterialTheme.typography.titleLarge, color = Color.White)
            Text(
                message,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Text(
                "Starting in $secondsLeft s",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * A deliberately non-interactive pause before ending Focus Mode. It is not a confirmation:
 * after a short breathing interval the mode is switched off automatically.
 */
@Composable
internal fun FocusModeExitPause(onFinished: () -> Unit) {
    var secondsLeft by remember { mutableStateOf(18) }
    val message = remember { ZenithCopy.focusExit.random() }
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
                message,
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
internal fun DragGhostOverlay(dragState: GridDragDropState, content: @Composable (id: String) -> Unit) {
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
internal fun EditModeDonePill(visible: Boolean, onDone: () -> Unit, modifier: Modifier = Modifier) {
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
internal fun isWidgetEnabled(id: String, visibility: com.zenith.launcher.data.model.WidgetVisibility): Boolean = when (id) {
    WidgetIds.DEADLINES -> visibility.deadlinesEnabled
    WidgetIds.FOCUS_MODE -> visibility.focusModeEnabled
    WidgetIds.POMODORO -> visibility.pomodoroEnabled
    WidgetIds.TODO -> visibility.todoEnabled
    WidgetIds.BACKLOG -> visibility.backlogEnabled
    WidgetIds.LIBRARY -> visibility.libraryEnabled
    WidgetIds.TARGETS -> visibility.targetsEnabled
    WidgetIds.APP_SHORTCUTS -> visibility.appShortcutsEnabled
    WidgetIds.SYSTEM_STATUS -> visibility.systemStatusEnabled
    else -> false
}

@Composable
internal fun WidgetForId(
    id: String,
    state: HomeUiState,
    viewModel: HomeViewModel,
    onShowAddTodo: () -> Unit,
    onEditTodo: (com.zenith.launcher.data.model.TodoItem) -> Unit,
    onShowAddBacklog: () -> Unit,
    onEditBacklog: (com.zenith.launcher.data.model.BacklogItem) -> Unit,
    onShowAddDeadline: () -> Unit,
    onShowAddLibrary: () -> Unit,
    onFocusToggle: () -> Unit = {},
    onPomodoroRunningChanged: (Boolean) -> Unit = {},
    onPomodoroProtectionChanged: (Boolean) -> Unit = {},
    onPomodoroPauseChanged: (Boolean, Int, String) -> Unit = { _, _, _ -> }
) {
    when (id) {
        WidgetIds.DEADLINES -> DeadlinesWidget(state.deadlineCountdowns, onAddClick = onShowAddDeadline)
        WidgetIds.FOCUS_MODE -> FocusModeToggle(state.isFocusModeActive, onFocusToggle)
        WidgetIds.POMODORO -> PomodoroWidget(
            onPomodoroRunningChanged = onPomodoroRunningChanged,
            onPomodoroProtectionChanged = onPomodoroProtectionChanged,
            onPomodoroPauseChanged = onPomodoroPauseChanged
        )
        WidgetIds.TODO -> TodoWidget(
            items = state.todoItems,
            onAddClick = onShowAddTodo,
            onToggle = viewModel::toggleTodo,
            onDelete = viewModel::deleteTodo,
            onEdit = onEditTodo
        )
        WidgetIds.BACKLOG -> BacklogWidget(
            items = state.backlogItems,
            onAddClick = onShowAddBacklog,
            onDelete = viewModel::deleteBacklogItem,
            onEdit = onEditBacklog
        )
        WidgetIds.LIBRARY -> LibraryWidget(
            links = state.libraryLinks,
            onAddClick = onShowAddLibrary,
            onDelete = viewModel::deleteLibraryLink
        )
        WidgetIds.TARGETS -> TargetsWidget(
            targets = state.studyTargets,
            onAdd = viewModel::addStudyTarget,
            onChange = viewModel::updateStudyTarget,
            onDelete = viewModel::deleteStudyTarget
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
