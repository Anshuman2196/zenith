package com.zenith.launcher.ui.home

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.zenith.launcher.data.model.BackgroundSettings
import com.zenith.launcher.data.model.WidgetIds
import com.zenith.launcher.ui.home.components.AddChapterDialog
import com.zenith.launcher.ui.home.components.AddPdfDialog
import com.zenith.launcher.ui.home.components.AddTodoDialog
import com.zenith.launcher.ui.home.components.AppDrawerOverlay
import com.zenith.launcher.ui.home.components.ChapterBacklogWidget
import com.zenith.launcher.ui.home.components.CountdownWidget
import com.zenith.launcher.ui.home.components.FocusModeToggle
import com.zenith.launcher.ui.home.components.GreetingHeader
import com.zenith.launcher.ui.home.components.PdfLauncherWidget
import com.zenith.launcher.ui.home.components.PomodoroWidget
import com.zenith.launcher.ui.home.components.TodoWidget
import com.zenith.launcher.ui.home.components.gridDragToReorder
import com.zenith.launcher.ui.home.components.rememberGridDragDropState
import com.zenith.launcher.ui.home.components.reportColumnBounds

/** How many columns the widget grid lays widgets out into - matches the reference design. */
private const val GRID_COLUMN_COUNT = 3

/** Cumulative horizontal drag (px) needed before a right-edge swipe counts as "open the drawer". */
private const val DRAWER_SWIPE_OPEN_THRESHOLD_PX = 60f

/**
 * The launcher's home screen: a live clock + greeting header, a hold-and-drag reorderable
 * 3-column widget grid, and a right-edge swipe (right-to-left) that opens the App Drawer as its
 * own full-screen area (see [AppDrawerOverlay]) - installed apps never live inside this grid.
 */
@Composable
fun HomeScreen(viewModel: HomeViewModel, onOpenSettings: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    var showAddTodoDialog by remember { mutableStateOf(false) }
    var showAddChapterDialog by remember { mutableStateOf(false) }
    var showAddPdfDialog by remember { mutableStateOf(false) }
    var isDrawerOpen by remember { mutableStateOf(false) }

    // Closing the app drawer with system back takes priority over the launcher's normal
    // "swallow back" behaviour, only while the drawer is actually open.
    BackHandler(enabled = isDrawerOpen) { isDrawerOpen = false }

    Box(modifier = Modifier.fillMaxSize()) {
        HomeBackground(background = state.background)

        Column(modifier = Modifier.fillMaxSize()) {
            GreetingHeader(greeting = state.greeting, onSettingsClick = onOpenSettings)

            val visibleColumns = remember(state.widgetColumns, state.widgetVisibility) {
                state.widgetColumns.map { column ->
                    column.filter { id -> isWidgetEnabled(id, state.widgetVisibility) }
                }
            }
            val dragState = rememberGridDragDropState(columns = visibleColumns) { id, toColumn, toIndex ->
                viewModel.moveWidget(id, toColumn, toIndex)
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp),
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
                                        .fillMaxWidth()
                                        .graphicsLayer {
                                            val offset = if (isDragging) dragState.draggingItemOffset else Offset.Zero
                                            translationX = offset.x
                                            translationY = offset.y
                                            val scale = if (isDragging) 1.04f else 1f
                                            scaleX = scale
                                            scaleY = scale
                                        }
                                        .zIndex(if (isDragging) 1f else 0f)
                                        .alpha(if (isDragging) 0.92f else 1f)
                                        .gridDragToReorder(dragState, id)
                                ) {
                                    WidgetForId(
                                        id = id,
                                        state = state,
                                        viewModel = viewModel,
                                        onShowAddTodo = { showAddTodoDialog = true },
                                        onShowAddChapter = { showAddChapterDialog = true },
                                        onShowAddPdf = { showAddPdfDialog = true }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            AppDrawerHandle(onOpen = { isDrawerOpen = true })
        }

        // Invisible strip along the right edge of the screen: swiping right-to-left starting
        // from here opens the App Drawer, mirroring how edge swipes work elsewhere on Android
        // without hijacking horizontal gestures used by widgets in the middle of the screen.
        if (!isDrawerOpen) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(24.dp)
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
                onLaunch = { app -> viewModel.launchApp(app); isDrawerOpen = false },
                onDismiss = { isDrawerOpen = false }
            )
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

/** Paints the chosen photo (cropped to fill) or solid color behind everything else on Home. */
@Composable
private fun HomeBackground(background: BackgroundSettings) {
    val context = LocalContext.current

    when {
        background.imageUri != null -> {
            val bitmap by produceState<ImageBitmap?>(initialValue = null, key1 = background.imageUri) {
                value = runCatching {
                    context.contentResolver.openInputStream(android.net.Uri.parse(background.imageUri))?.use {
                        BitmapFactory.decodeStream(it)?.asImageBitmap()
                    }
                }.getOrNull()
            }
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                bitmap?.let {
                    Image(
                        bitmap = it,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Subtle scrim so widget text stays readable over any photo.
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.35f))
                    )
                }
            }
        }
        background.colorArgb != null -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(background.colorArgb))
            )
        }
        else -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            )
        }
    }
}

/**
 * Small tappable hint at the bottom of Home pointing at the App Drawer. The actual gesture to
 * open it is a right-to-left swipe from the screen's right edge (see the edge-swipe strip in
 * [HomeScreen]); this row is a tap-friendly fallback plus a visible hint of that gesture.
 */
@Composable
private fun AppDrawerHandle(onOpen: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp, top = 4.dp)
            .clickable(onClick = onOpen)
    ) {
        Box(
            modifier = Modifier
                .size(width = 40.dp, height = 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f))
        )
        Spacer(Modifier.height(4.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = "Open apps",
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.size(16.dp)
        )
        Text(
            "Swipe left for apps",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
    }
}

private fun isWidgetEnabled(id: String, visibility: com.zenith.launcher.data.model.WidgetVisibility): Boolean = when (id) {
    WidgetIds.COUNTDOWN -> visibility.countdownEnabled
    WidgetIds.FOCUS_MODE -> visibility.focusModeEnabled
    WidgetIds.POMODORO -> visibility.pomodoroEnabled
    WidgetIds.TODO -> visibility.todoEnabled
    WidgetIds.CHAPTER_BACKLOG -> visibility.chapterBacklogEnabled
    WidgetIds.PDF_LAUNCHER -> visibility.pdfLauncherEnabled
    else -> false
}

@Composable
private fun WidgetForId(
    id: String,
    state: HomeUiState,
    viewModel: HomeViewModel,
    onShowAddTodo: () -> Unit,
    onShowAddChapter: () -> Unit,
    onShowAddPdf: () -> Unit
) {
    when (id) {
        WidgetIds.COUNTDOWN -> CountdownWidget(state.jeeMainDaysLeft, state.jeeAdvancedDaysLeft)
        WidgetIds.FOCUS_MODE -> FocusModeToggle(state.isFocusModeActive, viewModel::toggleFocusMode)
        WidgetIds.POMODORO -> PomodoroWidget()
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
    }
}
