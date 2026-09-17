package com.zenith.launcher.ui.home.components
import androidx.compose.runtime.getValue
import android.net.Uri
import android.app.WallpaperManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.zenith.launcher.data.model.BackgroundSettings
import com.zenith.launcher.util.BitmapUtils
import androidx.core.graphics.drawable.toBitmap


/**
 * Paints the chosen photo (cropped to fill) or solid color behind everything else - shared by
 * Home and the App Drawer, so both feel like one continuous surface
 * rather than the drawer/deck popping up over a plain color.
 *
 * The photo is decoded once at roughly screen resolution via [BitmapUtils] (not full camera
 * resolution) and reused as long as [background] doesn't change, which matters most on low-RAM
 * devices where decoding a 12MP+ photo at full size is the single biggest avoidable memory hit
 * in this app.
 */
@Composable
fun HomeBackground(background: BackgroundSettings, content: @Composable BoxScope.() -> Unit = {}) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current

    when {
        background.imageUri != null -> {
            val targetWidthPx = with(density) { configuration.screenWidthDp.dp.roundToPx() }
            val targetHeightPx = with(density) { configuration.screenHeightDp.dp.roundToPx() }
            val bitmap by produceState<ImageBitmap?>(initialValue = null, key1 = background.imageUri) {
                value = BitmapUtils.decodeSampledBitmapFromUri(
                    context = context,
                    uri = Uri.parse(background.imageUri),
                    reqWidth = targetWidthPx,
                    reqHeight = targetHeightPx
                )?.asImageBitmap()
            }
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                bitmap?.let {
                    Image(
                        bitmap = it,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Subtle scrim so widget/app-icon text stays readable over any photo.
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)))
                }
                content()
            }
        }
        background.colorArgb != null -> {
            Box(modifier = Modifier.fillMaxSize().background(Color(background.colorArgb)), content = content)
        }
        else -> {
            // Do not replace the phone's wallpaper with a launcher colour on first run. A custom
            // image or colour above remains an explicit opt-in override.
            val wallpaper by produceState<ImageBitmap?>(initialValue = null, key1 = context) {
                value = runCatching { WallpaperManager.getInstance(context).drawable?.toBitmap()?.asImageBitmap() }.getOrNull()
            }
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                wallpaper?.let {
                    Image(
                        bitmap = it,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                content()
            }
        }
    }
}
