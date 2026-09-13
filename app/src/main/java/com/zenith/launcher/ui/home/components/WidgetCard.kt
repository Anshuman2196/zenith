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
 * IMPORTANT: Material3's `Card` normally figures out its own text/icon color automatically from
 * the container color via `contentColorFor(containerColor)` - but that lookup only recognises
 * *exact* palette colors. The moment the container becomes `surface.copy(alpha = ...)` for the
 * glass effect, it no longer matches anything and Material3 silently falls back to whatever
 * (unrelated, often dim) color happened to be ambient outside the card. That's what was making
 * titles and the Pomodoro digits look washed-out even with no photo involved: any `Text()` inside
 * a widget that didn't set its own explicit color was inheriting that wrong fallback. Passing
 * [contentColor] explicitly below bypasses that broken auto-detection entirely.
 */
@Composable
fun WidgetCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val isDarkSurface = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    // Kept fairly low so the card stays genuinely see-through (glass, not a flat opaque panel) -
    // safe to do now that contentColor below is fixed, since legibility no longer depends on
    // cranking the container opacity way up.
    val containerAlpha = if (isDarkSurface) 0.55f else 0.62f

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = containerAlpha),
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        // A soft light rim on every edge - the classic glass/highlight look - rather than a dark
        // outline, which reads as a normal flat card border instead of a pane of glass.
        border = BorderStroke(1.dp, Color.White.copy(alpha = if (isDarkSurface) 0.10f else 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}
