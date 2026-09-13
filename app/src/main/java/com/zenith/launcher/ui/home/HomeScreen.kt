package com.zenith.launcher.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.zenith.launcher.data.model.AppInfo
import com.zenith.launcher.ui.home.components.AddChapterDialog
import com.zenith.launcher.ui.home.components.AddPdfDialog
import com.zenith.launcher.ui.home.components.AddTodoDialog
import com.zenith.launcher.ui.home.components.ChapterBacklogWidget
import com.zenith.launcher.ui.home.components.CountdownWidget
import com.zenith.launcher.ui.home.components.FocusModeToggle
import com.zenith.launcher.ui.home.components.GreetingHeader
import com.zenith.launcher.ui.home.components.PdfLauncherWidget
import com.zenith.launcher.ui.home.components.PomodoroWidget
import com.zenith.launcher.ui.home.components.TodoWidget

/**
 * The launcher's home screen: greeting header, toggleable widgets, and the app grid.
 * Reachable via the Settings gear icon OR a long-press anywhere on the background,
 * matching a stock launcher's UX.
 */
@Composable
fun HomeScreen(viewModel: HomeViewModel, onOpenSettings: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    var showAddTodoDialog by remember { mutableStateOf(false) }
    var showAddChapterDialog by remember { mutableStateOf(false) }
    var showAddPdfDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) {
                detectTapGestures(onLongPress = { onOpenSettings() })
            }
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Header + widgets occupy the full grid width as one spanning item.
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    GreetingHeader(greeting = state.greeting, onSettingsClick = onOpenSettings)

                    if (state.widgetVisibility.countdownEnabled) {
                        CountdownWidget(state.jeeMainDaysLeft, state.jeeAdvancedDaysLeft)
                    }
                    if (state.widgetVisibility.focusModeEnabled) {
                        FocusModeToggle(state.isFocusModeActive, viewModel::toggleFocusMode)
                    }
                    if (state.widgetVisibility.pomodoroEnabled) {
                        PomodoroWidget()
                    }
                    if (state.widgetVisibility.todoEnabled) {
                        TodoWidget(
                            items = state.todoItems,
                            onAddClick = { showAddTodoDialog = true },
                            onToggle = viewModel::toggleTodo,
                            onDelete = viewModel::deleteTodo
                        )
                    }
                    if (state.widgetVisibility.chapterBacklogEnabled) {
                        ChapterBacklogWidget(
                            items = state.chapterItems,
                            onAddClick = { showAddChapterDialog = true },
                            onDelete = viewModel::deleteChapter
                        )
                    }
                    if (state.widgetVisibility.pdfLauncherEnabled) {
                        PdfLauncherWidget(
                            links = state.pdfLinks,
                            onAddClick = { showAddPdfDialog = true },
                            onDelete = viewModel::deletePdfLink
                        )
                    }

                    Text(
                        text = "Apps",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            items(state.apps, key = { it.packageName + it.activityClassName }) { app ->
                AppIconCell(app = app, onClick = { viewModel.launchApp(app) })
            }
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

@Composable
private fun AppIconCell(app: AppInfo, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Image(
            bitmap = remember(app.packageName, app.activityClassName) { app.icon.toBitmap().asImageBitmap() },
            contentDescription = app.label,
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = app.label,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
