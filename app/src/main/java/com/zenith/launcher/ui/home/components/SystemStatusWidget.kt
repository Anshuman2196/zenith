package com.zenith.launcher.ui.home.components

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay

/**
 * Widget: a compact "at a glance" system status row - Wi-Fi connectivity, battery charge, and
 * (if one's connected) a Bluetooth device's name and, best-effort, its battery level.
 *
 * Double-tap the Wi-Fi row to jump straight to Wi-Fi settings, matching the quick-settings
 * shortcut behaviour Android users already expect from a status icon.
 */
@Composable
fun SystemStatusWidget() {
    val context = LocalContext.current

    WidgetCard {
        Text("Status", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            WifiStatus(context)
            BatteryStatus(context)
        }

        val bluetoothInfo = rememberConnectedBluetoothDevice()
        if (bluetoothInfo != null) {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = bluetoothInfo.name + (bluetoothInfo.batteryPercent?.let { " - $it%" } ?: ""),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ---------- Wi-Fi ----------

@Composable
private fun WifiStatus(context: Context) {
    var isConnected by remember { mutableStateOf(isWifiConnected(context)) }

    // Cheap poll rather than a registered NetworkCallback - this widget only needs to be
    // approximately live, and polling avoids managing a callback's lifecycle here.
    PollEvery(intervalMs = 5000) { isConnected = isWifiConnected(context) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(
                onDoubleTap = {
                    runCatching {
                        context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                }
            )
        }
    ) {
        Icon(
            imageVector = if (isConnected) Icons.Default.Wifi else Icons.Default.SignalWifiOff,
            contentDescription = if (isConnected) "Wi-Fi connected - double-tap for Wi-Fi settings" else "Wi-Fi disconnected - double-tap for Wi-Fi settings",
            modifier = Modifier.size(20.dp),
            tint = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(6.dp))
        Text(
            if (isConnected) "Wi-Fi" else "Offline",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun isWifiConnected(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    val network = cm.activeNetwork ?: return false
    val capabilities = cm.getNetworkCapabilities(network) ?: return false
    return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
}

// ---------- Battery ----------

@Composable
private fun BatteryStatus(context: Context) {
    var percent by remember { mutableStateOf(readBatteryPercent(context)) }
    var isCharging by remember { mutableStateOf(readIsCharging(context)) }

    PollEvery(intervalMs = 15000) {
        percent = readBatteryPercent(context)
        isCharging = readIsCharging(context)
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        if (isCharging) {
            Icon(Icons.Default.Bolt, contentDescription = "Charging", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        } else {
            Icon(Icons.Default.BatteryFull, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(4.dp))
        Text("$percent%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun readBatteryPercent(context: Context): Int {
    val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager ?: return 0
    return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).coerceIn(0, 100)
}

private fun readIsCharging(context: Context): Boolean {
    val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager ?: return false
    val status = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)
    return status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
}

// ---------- Bluetooth ----------

private data class ConnectedBluetoothInfo(val name: String, val batteryPercent: Int?)

@Composable
private fun rememberConnectedBluetoothDevice(): ConnectedBluetoothInfo? {
    val context = LocalContext.current
    var info by remember { mutableStateOf<ConnectedBluetoothInfo?>(null) }
    var hasPermission by remember { mutableStateOf(hasBluetoothConnectPermission(context)) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
    }

    // Ask once per widget lifetime rather than on every recomposition - if the user declines,
    // the Bluetooth row simply stays hidden instead of repeatedly re-prompting.
    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
    }

    PollEvery(intervalMs = 10000) {
        info = if (hasPermission) findConnectedBluetoothDevice(context) else null
    }

    return info
}

private fun hasBluetoothConnectPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

/**
 * Best-effort scan of bonded devices for one that's currently connected. There's no public,
 * stable API for "is this classic Bluetooth device connected right now" or "what's its battery
 * level" - both are read via reflection on hidden [BluetoothDevice] methods that exist on stock
 * Android but aren't guaranteed by every OEM, so any failure here just means the row stays
 * hidden rather than crashing.
 */
private fun findConnectedBluetoothDevice(context: Context): ConnectedBluetoothInfo? = runCatching {
    val adapter = BluetoothAdapter.getDefaultAdapter() ?: return null
    if (!adapter.isEnabled) return null

    val connected = adapter.bondedDevices.firstOrNull { device ->
        runCatching {
            val method = BluetoothDevice::class.java.getMethod("isConnected")
            method.invoke(device) as? Boolean ?: false
        }.getOrDefault(false)
    } ?: return null

    val battery = runCatching {
        val method = BluetoothDevice::class.java.getMethod("getBatteryLevel")
        (method.invoke(connected) as? Int)?.takeIf { it in 0..100 }
    }.getOrNull()

    ConnectedBluetoothInfo(name = connected.name ?: "Bluetooth device", batteryPercent = battery)
}.getOrNull()

/** Runs [action] immediately and then every [intervalMs] for as long as this composable is alive. */
@Composable
private fun PollEvery(intervalMs: Long, action: suspend () -> Unit) {
    val latestAction = rememberUpdatedState(action)
    LaunchedEffect(Unit) {
        while (true) {
            latestAction.value()
            delay(intervalMs)
        }
    }
}
