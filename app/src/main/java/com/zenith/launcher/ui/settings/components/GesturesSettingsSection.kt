package com.zenith.launcher.ui.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.zenith.launcher.util.SystemActionsHelper

/**
 * Settings section for Home's system-integration gestures. Right now that's just double-tap to
 * lock, which needs Device Admin (the only way a launcher can call
 * [android.app.admin.DevicePolicyManager.lockNow]) - this row explains that and walks the user
 * through granting it. The left-edge swipe for Recent Apps needs no setup at all: it opens
 * Zenith's own Recent Apps deck (see RecentAppsOverlay), not the system Overview screen, so
 * there's no special permission to grant for it.
 */
@Composable
fun GesturesSettingsSection(lockOnDoubleTap: Boolean, onLockOnDoubleTapChange: (Boolean) -> Unit) {
    val context = LocalContext.current
    var isAdminActive by remember { mutableStateOf(SystemActionsHelper.isDeviceAdminActive(context)) }

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
                        SystemActionsHelper.requestDeviceAdmin(context)
                        // The system's own grant dialog will report success/failure; re-check
                        // next time this screen is shown rather than guessing here.
                    }
                    onLockOnDoubleTapChange(checked)
                    isAdminActive = SystemActionsHelper.isDeviceAdminActive(context)
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

        Text("Swipe right from the left edge of Home for Recent Apps", style = MaterialTheme.typography.bodyMedium)
        Text(
            "Opens Zenith's own Recent Apps deck - no extra permission needed.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
