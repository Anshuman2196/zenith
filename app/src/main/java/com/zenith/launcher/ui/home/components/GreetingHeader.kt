package com.zenith.launcher.ui.home.components
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zenith.launcher.util.SystemActionsHelper
import com.zenith.launcher.data.model.ClockSize
import com.zenith.launcher.util.WeatherHelper
import com.zenith.launcher.util.WeatherInfo
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


/**
 * Top header, matching the reference design: the student's name is centered across the full
 * width of the screen (not just the leftover space next to the clock), while the live clock +
 * date and the gear icon into Settings sit pinned to the top-right corner as their own group.
 *
 * [overPhotoBackground] should be true whenever Home is showing a user photo behind it (see
 * [com.zenith.launcher.ui.home.HomeScreen]). A photo's brightness has nothing to do with whether
 * Light or Dark theme is selected, so instead of using the theme's `onBackground` color (which
 * can turn near-invisible - dark text on a dark photo, in Light theme) this header always renders
 * in a fixed light color with a soft shadow over a photo, matching how the darkening scrim behind
 * it is applied regardless of theme.
 *
 * [lockOnDoubleTap] (a Settings toggle) makes double-tapping anywhere in the header - the same
 * gesture several stock launchers use - lock the screen via [SystemActionsHelper.lockScreen].
 *
 * [settingsEnabled] disables (dims, but doesn't hide) the gear icon - used while a Pomodoro
 * session has locked the rest of Home.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GreetingHeader(
    greeting: String,
    onSettingsClick: () -> Unit,
    overPhotoBackground: Boolean = false,
    lockOnDoubleTap: Boolean = false,
    settingsEnabled: Boolean = true,
    clockSize: ClockSize = ClockSize.LARGE,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val textColor = if (overPhotoBackground) Color.White else MaterialTheme.colorScheme.onBackground
    val secondaryTextColor = if (overPhotoBackground) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
    val textShadow = if (overPhotoBackground) {
        Shadow(color = Color.Black.copy(alpha = 0.6f), blurRadius = 12f)
    } else {
        null
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 24.dp, start = 20.dp, end = 8.dp, bottom = 4.dp)
            .then(if (lockOnDoubleTap) Modifier.combinedClickable(onClick = {}, onDoubleClick = { SystemActionsHelper.lockScreen(context) }) else Modifier)
    ) {
        WeatherStatus(
            textColor = textColor,
            secondaryTextColor = secondaryTextColor,
            modifier = Modifier.align(Alignment.CenterStart)
        )
        Text(
            text = greeting,
            style = MaterialTheme.typography.headlineMedium.copy(shadow = textShadow),
            color = textColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                // Reserves roughly the width of the (now-larger) clock + gear group on the right
                // so a long greeting phrase truncates with an ellipsis instead of drawing under it.
                .padding(start = 86.dp, end = 132.dp)
        )

        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            InlineClock(timeColor = textColor, dateColor = secondaryTextColor, size = clockSize)
            IconButton(onClick = onSettingsClick, enabled = settingsEnabled) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Zenith settings",
                    tint = textColor
                )
            }
        }
    }
}

@Composable
private fun InlineClock(timeColor: Color, dateColor: Color, size: ClockSize) {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(1000)
        }
    }

    Column(horizontalAlignment = Alignment.End) {
        Text(
            text = now.format(DateTimeFormatter.ofPattern("h:mm a")),
            style = when (size) {
                ClockSize.SMALL -> MaterialTheme.typography.labelLarge
                ClockSize.MEDIUM -> MaterialTheme.typography.headlineSmall
                ClockSize.LARGE -> MaterialTheme.typography.displaySmall
            },
            color = timeColor
        )
        Text(
            text = now.format(DateTimeFormatter.ofPattern("dd MMM")),
            style = when (size) {
                ClockSize.SMALL -> MaterialTheme.typography.labelSmall
                ClockSize.MEDIUM, ClockSize.LARGE -> MaterialTheme.typography.bodyMedium
            },
            color = dateColor
        )
    }
}

/**
 * Live current-temperature reading, replacing what used to be a permanent "—°" placeholder. Asks
 * for coarse location once per Home visit (declining just leaves the placeholder showing - it's
 * never asked again until the process restarts, same as [SystemStatusWidget]'s Bluetooth
 * permission), then refreshes every 30 minutes via [WeatherHelper] - the only network call
 * anywhere in Zenith. See the README's Permissions section.
 */
@Composable
private fun WeatherStatus(textColor: Color, secondaryTextColor: Color, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var hasLocationPermission by remember { mutableStateOf(WeatherHelper.hasLocationPermission(context)) }
    var weather by remember { mutableStateOf<WeatherInfo?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasLocationPermission = granted
    }
    LaunchedEffect(Unit) {
        if (!hasLocationPermission) permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    LaunchedEffect(hasLocationPermission) {
        if (!hasLocationPermission) return@LaunchedEffect
        while (true) {
            weather = WeatherHelper.fetchCurrentWeather(context) ?: weather
            delay(30 * 60 * 1000L)
        }
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = weather?.let { WeatherHelper.iconFor(it.weatherCode) } ?: Icons.Default.WbSunny,
            contentDescription = "Weather",
            tint = textColor,
            modifier = Modifier.size(18.dp)
        )
        Column {
            Text("Weather", style = MaterialTheme.typography.labelSmall, color = secondaryTextColor)
            Text(
                text = weather?.let { "${it.temperatureCelsius.roundToInt()}°" } ?: "—°",
                style = MaterialTheme.typography.bodyMedium,
                color = textColor
            )
        }
    }
}
