package com.zenith.launcher.ui.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

/**
 * Shared visual wrapper for every home-screen widget - a translucent, rounded glass-like card
 * so widgets read clearly over a photo background (see [com.zenith.launcher.ui.home.HomeScreen])
 * while still looking cohesive against a plain color background.
 *
 * The Home photo background is often a dark or busy image regardless of which app theme is
 * active. A ~40%-see-through *white* card (Light theme's `surface`) over a dark photo reads as a
 * muddy grey smudge rather than glass, so the opacity and border here are tuned separately per
 * theme instead of using one fixed alpha for both.
 */
@Composable
fun WidgetCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val isDarkSurface = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val containerAlpha = if (isDarkSurface) 0.6f else 0.85f
    val borderColor = if (isDarkSurface) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.10f)

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = containerAlpha)),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}
