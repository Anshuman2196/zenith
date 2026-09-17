package com.zenith.launcher.ui.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.zenith.launcher.data.model.AppInfo
import com.zenith.launcher.ui.home.ZenithCopy

/** Pinned apps with explicit management instead of destructive long-press removal. */
@Composable
fun AppShortcutsWidget(
    pinnedApps: List<AppInfo>,
    allApps: List<AppInfo>,
    onLaunch: (AppInfo) -> Unit,
    onPin: (AppInfo) -> Unit,
    onUnpin: (AppInfo) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }
    var showManage by remember { mutableStateOf(false) }

    WidgetCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("App Shortcuts", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { showManage = true }, enabled = pinnedApps.isNotEmpty()) { Text("Manage") }
                AddShortcutTile(onClick = { showPicker = true })
            }
        }
        Spacer(Modifier.height(6.dp))

        if (pinnedApps.isEmpty()) {
            Text(ZenithCopy.emptyShortcuts.random(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LazyColumn(
                modifier = Modifier.heightIn(max = 150.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                items(pinnedApps, key = { it.packageName + it.activityClassName }) { app ->
                    ShortcutRow(app) { onLaunch(app) }
                }
            }
        }
    }

    if (showPicker) {
        PickAppDialog(
            apps = allApps.filterNot { app -> pinnedApps.any { it.packageName == app.packageName && it.activityClassName == app.activityClassName } },
            onDismiss = { showPicker = false },
            onPick = { onPin(it); showPicker = false }
        )
    }
    if (showManage) {
        ManageShortcutsDialog(pinnedApps, onDismiss = { showManage = false }, onRemove = onUnpin)
    }
}

@Composable
private fun ShortcutRow(app: AppInfo, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            bitmap = remember(app.packageName, app.activityClassName) { app.icon.toBitmap().asImageBitmap() },
            contentDescription = app.label,
            modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp))
        )
        Spacer(Modifier.width(10.dp))
        Text(app.label, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun AddShortcutTile(onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(42.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(Icons.Default.Add, contentDescription = "Add app shortcut") }
}

@Composable
private fun ManageShortcutsDialog(apps: List<AppInfo>, onDismiss: () -> Unit, onRemove: (AppInfo) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage shortcuts") },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(apps, key = { it.packageName + it.activityClassName }) { app ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Image(bitmap = remember(app.packageName, app.activityClassName) { app.icon.toBitmap().asImageBitmap() }, contentDescription = null, modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)))
                        Spacer(Modifier.width(10.dp))
                        Text(app.label, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        IconButton(onClick = { onRemove(app) }) { Icon(Icons.Default.DeleteOutline, contentDescription = "Remove ${app.label}") }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}

@Composable
private fun PickAppDialog(apps: List<AppInfo>, onDismiss: () -> Unit, onPick: (AppInfo) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add app shortcut") },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                if (apps.isEmpty()) item { Text("Every installed app is already pinned.") }
                items(apps, key = { it.packageName + it.activityClassName }) { app ->
                    Row(Modifier.fillMaxWidth().clickable { onPick(app) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Image(bitmap = remember(app.packageName, app.activityClassName) { app.icon.toBitmap().asImageBitmap() }, contentDescription = null, modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)))
                        Spacer(Modifier.width(12.dp))
                        Text(app.label)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
