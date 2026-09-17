package com.zenith.launcher.ui.settings.components
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.io.File
import com.zenith.launcher.data.model.FontChoice
import com.zenith.launcher.ui.theme.displayName
import com.zenith.launcher.ui.theme.toFontFamily


/**
 * Font picker: re-skins every piece of text in the launcher, including the Home screen greeting.
 * Each row is rendered in its own type face so the user sees a live preview before picking it.
 */
@Composable
fun FontSettingsSection(
    selected: FontChoice,
    customFontPath: String?,
    onSelect: (FontChoice) -> Unit,
    onImportFont: (String?) -> Unit
) {
    val context = LocalContext.current
    val importFont = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val extension = uri.lastPathSegment?.substringAfterLast('.', "ttf")?.lowercase()
                ?.takeIf { it == "ttf" || it == "otf" } ?: "ttf"
            val destination = File(context.filesDir, "custom-font.$extension")
            context.contentResolver.openInputStream(uri)?.use { input -> destination.outputStream().use(input::copyTo) }
                ?: error("Unable to read the selected font")
            onImportFont(destination.absolutePath)
        }
    }
    SettingsSectionCard(title = "Font") {
        Text(
            "Changes the type face used across the whole launcher, including the greeting.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column {
            FontChoice.entries.forEach { choice ->
                FontRow(choice = choice, isSelected = choice == selected, onClick = { onSelect(choice) })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.OutlinedButton(onClick = { importFont.launch(arrayOf("font/*", "application/font-sfnt", "application/octet-stream")) }) {
                Text("Import .ttf or .otf")
            }
            if (customFontPath != null) {
                TextButton(onClick = { onImportFont(null) }) { Text("Remove custom font") }
            }
        }
        Text(
            if (customFontPath == null) "Import a font file to use it across Zenith." else "Your imported font is active across Zenith.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FontRow(choice: FontChoice, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = isSelected, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RadioButton(selected = isSelected, onClick = onClick)
        Text(
            choice.displayName,
            style = MaterialTheme.typography.bodyLarge.copy(fontFamily = choice.toFontFamily())
        )
    }
}
