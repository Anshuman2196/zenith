package com.zenith.launcher.ui.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.zenith.launcher.data.model.AppInfo
import com.zenith.launcher.data.model.BackgroundSettings

/**
 * Zenith's own Recent Apps deck - opened with a left-to-right swipe from Home's left edge. This
 * is a deliberate design choice, not a limitation of this one screen: a third-party launcher has
 * no access to the system's actual task snapshots (the live thumbnails you'd see in stock
 * Android's Overview), so rather than fake that with static icons pretending to be app previews,
 * this shows what a launcher genuinely *can* offer well - a clean, fast list of apps you've
 * actually launched through Zenith, ordered most-recent-first, styled to match Home (same
 * wallpaper, same glass cards) instead of looking like a bolted-on system dialog.
 *
 * Removing an entry here only forgets it from this list - it does not (and cannot, without
 * system privilege) force-stop or clear the app's actual running state.
 */
@Composable
fun RecentAppsOverlay(
    recentApps: List<AppInfo>,
    background: BackgroundSettings,
    onLaunch: (AppInfo) -> Unit,
    onRemove: (AppInfo) -> Unit,
    onDismiss: () -> Unit
) {
    HomeBackground(background = background) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.15f))
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { _, dragAmount ->
                        if (dragAmount < -12f) onDismiss()
                    }
                }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Recent Apps", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                if (recentApps.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "Apps you open will show up here",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(recentApps, key = { it.packageName + it.activityClassName }) { app ->
                            RecentAppCard(app = app, onClick = { onLaunch(app) }, onRemove = { onRemove(app) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentAppCard(app: AppInfo, onClick: () -> Unit, onRemove: () -> Unit) {
    // Same "frosted glass" recipe as WidgetCard - a moderately translucent surface plus a soft
    // light rim - kept consistent everywhere the wallpaper shows through, not just Home.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.14f))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            bitmap = remember(app.packageName, app.activityClassName) { app.icon.toBitmap().asImageBitmap() },
            contentDescription = null,
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
        )
        Spacer(Modifier.width(12.dp))
        Text(
            app.label,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onRemove) {
            Icon(Icons.Default.Close, contentDescription = "Remove from Recents", tint = Color.White.copy(alpha = 0.7f))
        }
    }
}
