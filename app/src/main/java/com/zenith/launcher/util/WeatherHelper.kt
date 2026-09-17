package com.zenith.launcher.util
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.resume


/** A single current-conditions reading for the Home screen header's weather row. */
data class WeatherInfo(val temperatureCelsius: Double, val weatherCode: Int)

/**
 * Current-conditions weather for the Home header. This is the one place in Zenith that touches
 * the network (see the README's Permissions section) - it uses the device's coarse location and
 * Open-Meteo's free, key-less forecast API, nothing else.
 *
 * There's no bundled location library here (no Play Services / Fused Location dependency) -
 * just the framework [LocationManager], matching the rest of the app's preference for framework
 * APIs over extra dependencies (see [SystemActionsHelper], [DefaultHomeHelper]).
 */
object WeatherHelper {

    fun hasLocationPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * A cached network-provider fix if one exists (instant), otherwise a single fresh one (a few
     * seconds). Coarse/network location only - accurate enough for a temperature reading, and
     * keeps this feature to [Manifest.permission.ACCESS_COARSE_LOCATION] rather than needing fine
     * location or a GPS fix.
     */
    private suspend fun currentLocation(context: Context, timeoutMs: Long = 10_000): Location? {
        if (!hasLocationPermission(context)) return null
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null

        val cached = runCatching { locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) }.getOrNull()
        if (cached != null) return cached

        if (!locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) return null

        return withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine { continuation ->
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        if (continuation.isActive) continuation.resume(location)
                        runCatching { locationManager.removeUpdates(this) }
                    }
                }
                val requested = runCatching {
                    locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, listener, Looper.getMainLooper())
                }
                if (requested.isFailure && continuation.isActive) continuation.resume(null)
                continuation.invokeOnCancellation { runCatching { locationManager.removeUpdates(listener) } }
            }
        }
    }

    /** Fetches a fresh reading, or null on any failure (no permission, no fix, no connectivity). */
    suspend fun fetchCurrentWeather(context: Context): WeatherInfo? = withContext(Dispatchers.IO) {
        val location = currentLocation(context) ?: return@withContext null
        runCatching {
            val url = URL(
                "https://api.open-meteo.com/v1/forecast?latitude=${location.latitude}&longitude=${location.longitude}" +
                    "&current=temperature_2m,weather_code&temperature_unit=celsius"
            )
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 8_000
            connection.readTimeout = 8_000
            try {
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                val current = Json.parseToJsonElement(body).jsonObject["current"]?.jsonObject
                val temperature = current?.get("temperature_2m")?.jsonPrimitive?.double
                val code = current?.get("weather_code")?.jsonPrimitive?.int ?: 0
                temperature?.let { WeatherInfo(it, code) }
            } finally {
                connection.disconnect()
            }
        }.getOrNull()
    }

    /** Maps Open-Meteo's WMO weather codes to a representative Material icon. */
    fun iconFor(weatherCode: Int): ImageVector = when (weatherCode) {
        0, 1 -> Icons.Default.WbSunny
        in 2..3, 45, 48 -> Icons.Default.Cloud
        in 51..67, in 80..82 -> Icons.Default.Umbrella
        in 71..77, in 85..86 -> Icons.Default.AcUnit
        in 95..99 -> Icons.Default.Thunderstorm
        else -> Icons.Default.WbSunny
    }
}
