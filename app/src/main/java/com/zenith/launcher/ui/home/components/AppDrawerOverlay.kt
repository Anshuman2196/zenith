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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
 * Apps are grouped under user-managed category headers. A long-press can change the category,
 * pin the app, or classify it for Focus Mode / Distractions without leaving the drawer.
 */
@Composable
fun AppDrawerOverlay(
    apps: List<AppInfo>,
    categories: Map<String, String>,
    categoryTypes: List<String>,
    background: BackgroundSettings,
    onLaunch: (AppInfo) -> Unit,
    onDismiss: () -> Unit,
    onUninstall: (AppInfo) -> Unit,
    canUninstall: (AppInfo) -> Boolean,
    onOpenAppInfo: (AppInfo) -> Unit,
    onSetCategory: (AppInfo, String) -> Unit,
    onPinShortcut: (AppInfo) -> Unit,
    focusAllowedPackages: Set<String>,
    distractionPackages: Set<String>,
    onToggleFocusAllowed: (AppInfo) -> Unit,
    onToggleDistraction: (AppInfo) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var menuApp by remember { mutableStateOf<AppInfo?>(null) }

    val filtered = remember(apps, query) {
        if (query.isBlank()) apps else apps.filter { it.label.contains(query, ignoreCase = true) }
    }

    val groups = remember(filtered, categories, categoryTypes, query) {
        if (query.isNotBlank()) {
            listOf("Search results" to filtered)
        } else {
            categoryTypes.map { type ->
                type to filtered.filter {
                    (categories[it.packageName] ?: AppCategory.OTHER.displayName) == type
                }
            }.filter { it.second.isNotEmpty() }
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
            androidx.compose.foundation.layout.BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
            ) {
                val twoColumns = maxWidth >= 700.dp
                val leftGroups = if (twoColumns) groups.filterIndexed { index, _ -> index % 2 == 0 } else groups
                val rightGroups = if (twoColumns) groups.filterIndexed { index, _ -> index % 2 == 1 } else emptyList()

                Column(modifier = Modifier.fillMaxSize()) {
                    DrawerSearchBar(
                        query = query,
                        onQueryChange = { query = it },
                        onClose = onDismiss
                    )

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(top = 8.dp, bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(if (twoColumns) 16.dp else 0.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        DrawerCategoryColumn(
                            groups = leftGroups,
                            onLaunch = onLaunch,
                            onLongClick = { menuApp = it },
                            modifier = Modifier.weight(1f)
                        )
                        if (twoColumns) {
                            DrawerCategoryColumn(
                                groups = rightGroups,
                                onLaunch = onLaunch,
                                onLongClick = { menuApp = it },
                                modifier = Modifier.weight(1f)
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
            currentCategory = categories[app.packageName] ?: AppCategory.OTHER.displayName,
            categoryTypes = categoryTypes,
            focusAllowed = app.packageName in focusAllowedPackages,
            isDistraction = app.packageName in distractionPackages,
            canUninstall = canUninstall(app),
            onDismiss = { menuApp = null },
            onPin = { onPinShortcut(app); menuApp = null },
            onAppInfo = { onOpenAppInfo(app); menuApp = null },
            onUninstall = { onUninstall(app); menuApp = null },
            onSetCategory = { onSetCategory(app, it); menuApp = null },
            onToggleFocusAllowed = { onToggleFocusAllowed(app) },
            onToggleDistraction = { onToggleDistraction(app) }
        )
    }
}

@Composable
private fun DrawerCategoryColumn(
    groups: List<Pair<String, List<AppInfo>>>,
    onLaunch: (AppInfo) -> Unit,
    onLongClick: (AppInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        groups.forEach { (category, appsInCategory) ->
            DrawerCategoryCard(
                category = category,
                apps = appsInCategory,
                onLaunch = onLaunch,
                onLongClick = onLongClick
            )
        }
    }
}

@Composable
private fun DrawerCategoryCard(
    category: String,
    apps: List<AppInfo>,
    onLaunch: (AppInfo) -> Unit,
    onLongClick: (AppInfo) -> Unit
) {
    Column {
        Text(
            text = category,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            modifier = Modifier.padding(start = 6.dp, bottom = 6.dp)
        )
        androidx.compose.material3.Card(
            shape = RoundedCornerShape(20.dp),
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.58f),
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
            elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                apps.chunked(3).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        row.forEach { app ->
                            Box(Modifier.weight(1f)) {
                                DrawerAppIconCell(
                                    app = app,
                                    onClick = { onLaunch(app) },
                                    onLongClick = { onLongClick(app) }
                                )
                            }
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerSearchBar(query: String, onQueryChange: (String) -> Unit, onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .padding(start = 14.dp, end = 4.dp, top = 7.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.72f)
        )
        Spacer(Modifier.width(9.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    "Search apps",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.55f)
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = MaterialTheme.typography.bodyLarge.fontSize
                ),
                cursorBrush = SolidColor(Color.White),
                modifier = Modifier.fillMaxWidth()
            )
        }
        IconButton(onClick = onClose) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Close app drawer",
                tint = Color.White.copy(alpha = 0.82f)
            )
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
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 9.dp, horizontal = 3.dp)
    ) {
        Image(
            bitmap = remember(app.packageName, app.activityClassName) {
                app.icon.toBitmap().asImageBitmap()
            },
            contentDescription = app.label,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
        )
        Spacer(Modifier.height(5.dp))
        Text(
            text = app.label,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
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
    currentCategory: String,
    categoryTypes: List<String>,
    focusAllowed: Boolean,
    isDistraction: Boolean,
    canUninstall: Boolean,
    onDismiss: () -> Unit,
    onPin: () -> Unit,
    onAppInfo: () -> Unit,
    onUninstall: () -> Unit,
    onSetCategory: (String) -> Unit,
    onToggleFocusAllowed: () -> Unit,
    onToggleDistraction: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(app.label) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ActionRow(icon = Icons.Default.PushPin, label = "Pin to Home", onClick = onPin)
                ActionRow(icon = Icons.Default.Info, label = "App info", onClick = onAppInfo)

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text("Attention", style = MaterialTheme.typography.labelLarge)
                AttentionToggleRow(
                    label = "Available in Focus Mode",
                    checked = focusAllowed,
                    onCheckedChange = onToggleFocusAllowed
                )
                AttentionToggleRow(
                    label = "Distraction",
                    checked = isDistraction,
                    onCheckedChange = onToggleDistraction
                )

                Spacer(Modifier.height(8.dp))
                Text("Category", style = MaterialTheme.typography.labelLarge)
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categoryTypes.chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { option ->
                                FilterChip(
                                    selected = currentCategory == option,
                                    onClick = { onSetCategory(option) },
                                    label = { Text(option, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }
                }

                if (canUninstall) {
                    Spacer(Modifier.height(8.dp))
                    ActionRow(icon = Icons.Default.DeleteOutline, label = "Uninstall", onClick = onUninstall)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
private fun AttentionToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = { onCheckedChange() })
    }
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
