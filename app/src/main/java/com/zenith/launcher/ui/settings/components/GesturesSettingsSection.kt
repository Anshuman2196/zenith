package com.zenith.launcher.ui.settings.components
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.zenith.launcher.util.SystemActionsHelper


/** Settings section for Home's double-tap lock integration. Device Admin is required because it
 * is the only public route for a launcher to call DevicePolicyManager.lockNow. */
@Composable
fun GesturesSettingsSection(lockOnDoubleTap: Boolean, onLockOnDoubleTapChange: (Boolean) -> Unit) {
    val context = LocalContext.current
    var isAdminActive by remember { mutableStateOf(SystemActionsHelper.isDeviceAdminActive(context)) }
    var enableRequested by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    // Android's Device Admin grant UI is external; refresh as soon as the user returns instead
    // of leaving the switch in a stale state that makes double-tap appear broken.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isAdminActive = SystemActionsHelper.isDeviceAdminActive(context)
                if (enableRequested && isAdminActive) {
                    onLockOnDoubleTapChange(true)
                    enableRequested = false
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    SettingsSectionCard(title = "Gestures") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Double-tap Home to lock screen", style = MaterialTheme.typography.bodyMedium)
            Switch(
                checked = lockOnDoubleTap && isAdminActive,
                onCheckedChange = { checked ->
                    if (checked && !isAdminActive) {
                        enableRequested = true
                        SystemActionsHelper.requestDeviceAdmin(context)
                        // The system's own grant dialog will report success/failure; re-check
                        // next time this screen is shown rather than guessing here.
                    }
                    if (!checked || isAdminActive) onLockOnDoubleTapChange(checked)
                }
            )
        }
        Text(
            if (isAdminActive) {
                "Device Admin is active - only used for locking the screen."
            } else {
                "Turning this on will ask you to grant Device Admin - used only to lock the screen, nothing else."
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

    }
}
