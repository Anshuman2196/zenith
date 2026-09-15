package com.zenith.launcher.ui.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.zenith.launcher.data.model.AppCategory
import com.zenith.launcher.data.model.AppInfo
import com.zenith.launcher.data.model.BackgroundSettings
import com.zenith.launcher.data.model.displayName

/**
 * The app drawer: every installed app, in its own dedicated full-screen area rather than mixed
 * into the Home widget grid. Opened with a right-to-left swipe from Home's right edge - matching
 * how a stock Android launcher separates "widgets" from "all apps". Dismissed by the X button, a
 * swipe back to the right, or the system back gesture.
 *
 * Shares [background] with Home (see [HomeBackground]) and renders category headers and app
 * icons on the same frosted-glass cards used everywhere else in the app, so opening the drawer
 * feels like sliding a panel over the same surface rather than switching to a different screen.
 *
 * Apps are grouped under category headers (Study / Games / Social / Entertainment / Other, see
 * [AppCategory]) - every app defaults to Other until re-assigned, either here via long-press >
 * "Move to..." or in bulk from Settings > App Categories.
 */
@Composable
fun AppDrawerOverlay(
    apps: List<AppInfo>,
    categories: Map<String, AppCategory>,
    background: BackgroundSettings,
    onLaunch: (AppInfo) -> Unit,
    onDismiss: () -> Unit,
    onUninstall: (AppInfo) -> Unit,
    canUninstall: (AppInfo) -> Boolean,
    onOpenAppInfo: (AppInfo) -> Unit,
    onSetCategory: (AppInfo, AppCategory) -> Unit,
    onPinShortcut: (AppInfo) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var menuApp by remember { mutableStateOf<AppInfo?>(null) }

    val filtered = remember(apps, query) {
        if (query.isBlank()) apps else apps.filter { it.label.contains(query, ignoreCase = true) }
    }

    // While searching, category grouping just gets in the way of finding one specific app, so
    // it's dropped in favour of one flat alphabetical grid - grouping only applies to browsing.
    val entries = remember(filtered, categories, query) {
        if (query.isNotBlank()) {
            filtered.map { DrawerEntry.App(it) }
        } else {
            AppCategory.entries
                .map { category -> category to filtered.filter { (categories[it.packageName] ?: AppCategory.OTHER) == category } }
                .filter { (_, appsInCategory) -> appsInCategory.isNotEmpty() }
                .flatMap { (category, appsInCategory) ->
                    listOf(DrawerEntry.Header(category)) + appsInCategory.map { DrawerEntry.App(it) }
                }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        HomeBackground(background = background)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { _, dragAmount ->
                        if (dragAmount > 12f) onDismiss()
                    }
                }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Drag handle - swiping right anywhere below it also closes the drawer.
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .align(Alignment.CenterHorizontally)
                        .size(width = 40.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.4f))
                )

                DrawerSearchBar(query = query, onQueryChange = { query = it }, onClose = onDismiss)

                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        entries,
                        key = { entry ->
                            when (entry) {
                                is DrawerEntry.Header -> "header_${entry.category.name}"
                                is DrawerEntry.App -> entry.app.packageName + entry.app.activityClassName
                            }
                        },
                        span = { entry -> if (entry is DrawerEntry.Header) GridItemSpan(maxLineSpan) else GridItemSpan(1) }
                    ) { entry ->
                        when (entry) {
                            is DrawerEntry.Header -> CategoryHeader(entry.category)
                            is DrawerEntry.App -> DrawerAppIconCell(
                                app = entry.app,
                                onClick = { onLaunch(entry.app) },
                                onLongClick = { menuApp = entry.app }
                            )
                        }
                    }
                }
            }
        }
    }

    val app = menuApp
    if (app != null) {
        AppContextMenuDialog(
            app = app,
            currentCategory = categories[app.packageName] ?: AppCategory.OTHER,
            canUninstall = canUninstall(app),
            onDismiss = { menuApp = null },
            onPin = { onPinShortcut(app); menuApp = null },
            onAppInfo = { onOpenAppInfo(app); menuApp = null },
            onUninstall = { onUninstall(app); menuApp = null },
            onSetCategory = { onSetCategory(app, it); menuApp = null }
        )
    }
}

private sealed interface DrawerEntry {
    data class Header(val category: AppCategory) : DrawerEntry
    data class App(val app: AppInfo) : DrawerEntry
}

@Composable
private fun CategoryHeader(category: AppCategory) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.16f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(category.displayName, style = MaterialTheme.typography.titleMedium, color = Color.White)
    }
}

@Composable
private fun DrawerSearchBar(query: String, onQueryChange: (String) -> Unit, onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.14f))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.8f))
        Spacer(Modifier.width(8.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text("Search apps", style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.7f))
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = MaterialTheme.typography.bodyLarge.fontSize),
                cursorBrush = SolidColor(Color.White)
            )
        }
        IconButton(onClick = onClose) {
            Icon(Icons.Default.Close, contentDescription = "Close app drawer", tint = Color.White)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DrawerAppIconCell(app: AppInfo, onClick: () -> Unit, onLongClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.10f))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 10.dp, horizontal = 4.dp)
    ) {
        Image(
            bitmap = remember(app.packageName, app.activityClassName) { app.icon.toBitmap().asImageBitmap() },
            contentDescription = app.label,
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = app.label,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            color = Color.White
        )
    }
}

/**
 * Long-press action sheet for one app: pin it to Home's App Shortcuts widget, jump to its system
 * App Info page, move it to a different drawer category, or uninstall it (hidden for apps
 * [canUninstall] says can't be - system apps, matching the same rule Android itself enforces).
 */
@Composable
private fun AppContextMenuDialog(
    app: AppInfo,
    currentCategory: AppCategory,
    canUninstall: Boolean,
    onDismiss: () -> Unit,
    onPin: () -> Unit,
    onAppInfo: () -> Unit,
    onUninstall: () -> Unit,
    onSetCategory: (AppCategory) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(app.label) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ActionRow(icon = Icons.Default.PushPin, label = "Pin to Home", onClick = onPin)
                ActionRow(icon = Icons.Default.Info, label = "App info", onClick = onAppInfo)
                if (canUninstall) {
                    ActionRow(icon = Icons.Default.DeleteOutline, label = "Uninstall", onClick = onUninstall)
                }

                Spacer(Modifier.height(8.dp))
                Text("Move to...", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AppCategory.entries.forEach { option ->
                        FilterChip(
                            selected = currentCategory == option,
                            onClick = { onSetCategory(option) },
                            label = { Text(option.displayName, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
private fun ActionRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}
