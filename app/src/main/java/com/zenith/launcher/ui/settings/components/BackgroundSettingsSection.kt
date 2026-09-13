package com.zenith.launcher.ui.settings.components

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.zenith.launcher.data.model.BackgroundSettings

/** Preset swatches offered alongside "pick a photo" - kept close to the app's calm palette. */
private val PRESET_COLORS: List<Long> = listOf(
    0xFF000000L, // pure black, matches the app icon
    0xFF16181CL, // default charcoal
    0xFF1E2126L,
    0xFF2D5FA8L,
    0xFF3E7268L,
    0xFF5B5651L
)

/**
 * Background Customization: pick any photo from the device as the Home screen background,
 * or fall back to a flat color swatch. Mirrors how stock launchers let you set a wallpaper,
 * but scoped to just this launcher's Home screen rather than the system wallpaper.
 */
@Composable
fun BackgroundSettingsSection(
    background: BackgroundSettings,
    onPickImage: (String?) -> Unit,
    onPickColor: (Long?) -> Unit,
    onReset: () -> Unit
) {
    val context = LocalContext.current

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            onPickImage(uri.toString())
        }
    }

    SettingsSectionCard(title = "Home Background") {
        Text(
            "Use any photo, or a flat color, behind your widgets.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedButton(onClick = { pickImageLauncher.launch(arrayOf("image/*")) }) {
            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
            Text("  Choose a photo")
        }

        Text("Or pick a color", style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PRESET_COLORS.forEach { argb ->
                val isSelected = background.imageUri == null && background.colorArgb == argb
                ColorSwatch(
                    color = Color(argb),
                    isSelected = isSelected,
                    onClick = { onPickColor(argb) }
                )
            }
        }

        OutlinedButton(onClick = onReset) {
            Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
            Text("  Reset to default")
        }
    }
}

@Composable
private fun ColorSwatch(color: Color, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.2f),
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelected) {
            Icon(Icons.Default.Check, contentDescription = "Selected", tint = Color.White, modifier = Modifier.size(18.dp))
        }
    }
}
