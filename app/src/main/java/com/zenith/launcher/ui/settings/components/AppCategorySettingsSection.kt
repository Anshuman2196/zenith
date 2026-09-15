package com.zenith.launcher.ui.settings.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.zenith.launcher.data.model.AppCategory
import com.zenith.launcher.data.model.AppInfo
import com.zenith.launcher.data.model.displayName

/**
 * Settings section for App Drawer categorization: pick a category (Study / Games / Social /
 * Entertainment / Other) for every installed app. This is the "configurable" side of drawer
 * grouping - the same assignment can also be changed faster in the moment via long-press >
 * "Move to..." right inside the App Drawer (see AppDrawerOverlay).
 */
@Composable
fun AppCategorySettingsSection(
    apps: List<AppInfo>,
    categories: Map<String, String>,
    categoryTypes: List<String>,
    onSetCategory: (packageName: String, String) -> Unit,
    onAddCategory: (String) -> Unit,
    showCard: Boolean = true
) {
    var query by remember { mutableStateOf("") }
    var newCategory by remember { mutableStateOf("") }
    val filtered = remember(apps, query) {
        if (query.isBlank()) apps else apps.filter { it.label.contains(query, ignoreCase = true) }
    }

    val content: @Composable ColumnScope.() -> Unit = {
        Text(
            "Group apps in the App Drawer by how you use them.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newCategory,
                onValueChange = { newCategory = it },
                label = { Text("Add category") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = {
                onAddCategory(newCategory)
                newCategory = ""
            }, enabled = newCategory.isNotBlank()) { Text("Add") }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(8.dp))
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = MaterialTheme.typography.bodyMedium.fontSize),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Fixed height so this section doesn't try to grow to fit potentially hundreds of apps
        // inside Settings' own outer scroll - it scrolls independently instead.
        LazyColumn(modifier = Modifier.fillMaxWidth().height(320.dp)) {
            items(filtered, key = { it.packageName + it.activityClassName }) { app ->
                AppCategoryRow(
                    app = app,
                    category = categories[app.packageName] ?: AppCategory.OTHER.displayName,
                    categoryTypes = categoryTypes,
                    onSetCategory = { onSetCategory(app.packageName, it) }
                )
            }
        }
    }
    if (showCard) SettingsSectionCard(title = "App Categories", content = content) else content()
}

@Composable
private fun AppCategoryRow(app: AppInfo, category: String, categoryTypes: List<String>, onSetCategory: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                bitmap = remember(app.packageName, app.activityClassName) { app.icon.toBitmap().asImageBitmap() },
                contentDescription = null,
                modifier = Modifier.size(28.dp).clip(RoundedCornerShape(8.dp))
            )
            Spacer(Modifier.width(8.dp))
            Text(
                app.label,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            categoryTypes.forEach { option ->
                FilterChip(
                    selected = category == option,
                    onClick = { onSetCategory(option) },
                    label = { Text(option, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
        Spacer(Modifier.height(10.dp))
    }
}
