package com.zenith.launcher.ui.home

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
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
import com.zenith.launcher.ui.home.components.ClockWidget
import com.zenith.launcher.ui.home.components.CountdownWidget
import com.zenith.launcher.ui.home.components.FocusModeToggle
import com.zenith.launcher.ui.home.components.GreetingHeader
import com.zenith.launcher.ui.home.components.PdfLauncherWidget
import com.zenith.launcher.ui.home.components.PomodoroWidget
import com.zenith.launcher.ui.home.components.TodoWidget
import com.zenith.launcher.ui.home.components.dragToReorder
import com.zenith.launcher.ui.home.components.rememberDragDropListState

/**
 * The launcher's home screen: a live clock + greeting header, a hold-and-drag reorderable
 * column of widgets, and a swipe-up handle that opens the App Drawer as its own full-screen
 * area (see [AppDrawerOverlay]) - installed apps never live inside this scrolling widget list.
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

            val listState = rememberLazyListState()
            val visibleWidgetIds = remember(state.widgetOrder, state.widgetVisibility) {
                state.widgetOrder.filter { id -> isWidgetEnabled(id, state.widgetVisibility) }
            }
            val dragState = rememberDragDropListState(listState = listState) { from, to ->
                val fromId = visibleWidgetIds.getOrNull(from)
                val toId = visibleWidgetIds.getOrNull(to)
                if (fromId != null && toId != null) {
                    val fullFrom = state.widgetOrder.indexOf(fromId)
                    val fullTo = state.widgetOrder.indexOf(toId)
                    if (fullFrom >= 0 && fullTo >= 0) viewModel.moveWidget(fullFrom, fullTo)
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                itemsIndexed(visibleWidgetIds, key = { _, id -> id }) { index, id ->
                    val isDragging = dragState.isDragging(index)
                    Box(
                        modifier = Modifier
                            .graphicsLayer { translationY = if (isDragging) dragState.draggingItemOffset else 0f }
                            .zIndex(if (isDragging) 1f else 0f)
                            .alpha(if (isDragging) 0.92f else 1f)
                            .dragToReorder(dragState, index)
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

            AppDrawerHandle(onOpen = { isDrawerOpen = true })
        }

        AnimatedVisibility(
            visible = isDrawerOpen,
            enter = slideInVertically(initialOffsetY = { fullHeight -> fullHeight }),
            exit = slideOutVertically(targetOffsetY = { fullHeight -> fullHeight }),
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

/** Small pill at the bottom of Home - tap it, or drag it upward, to reveal the App Drawer. */
@Composable
private fun AppDrawerHandle(onOpen: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp, top = 4.dp)
            .clickable(onClick = onOpen)
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount < -12f) onOpen()
                }
            }
    ) {
        Box(
            modifier = Modifier
                .size(width = 40.dp, height = 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f))
        )
        Spacer(Modifier.height(4.dp))
        Icon(
            imageVector = Icons.Default.KeyboardArrowUp,
            contentDescription = "Open apps",
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.size(16.dp)
        )
        Text(
            "Apps",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
    }
}

private fun isWidgetEnabled(id: String, visibility: com.zenith.launcher.data.model.WidgetVisibility): Boolean = when (id) {
    WidgetIds.CLOCK -> visibility.clockEnabled
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
        WidgetIds.CLOCK -> ClockWidget()
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
