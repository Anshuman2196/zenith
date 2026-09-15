package com.zenith.launcher.util

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.zenith.launcher.service.ZenithDeviceAdminReceiver

/**
 * Double-tap-to-lock is the one system action this launcher needs that has no direct API - a
 * launcher can't lock the screen itself, only [DevicePolicyManager.lockNow] can, and that
 * requires the app to be an active Device Admin. This fails soft: if Device Admin isn't granted
 * yet, it walks the user through granting it instead of silently doing nothing.
 *
 */
object SystemActionsHelper {

    private fun adminComponent(context: Context) = ComponentName(context, ZenithDeviceAdminReceiver::class.java)

    fun isDeviceAdminActive(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager ?: return false
        return dpm.isAdminActive(adminComponent(context))
    }

    /**
     * Turns the screen off and locks it immediately - the same effect as pressing the power
     * button - via [DevicePolicyManager.lockNow]. Falls back to walking the user through
     * granting Device Admin if it isn't active yet, since that's the only way this call works.
     */
    fun lockScreen(context: Context) {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
        if (dpm != null && isDeviceAdminActive(context)) {
            runCatching { dpm.lockNow() }
        } else {
            requestDeviceAdmin(context)
        }
    }

    fun requestDeviceAdmin(context: Context) {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent(context))
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Zenith Launcher needs this to lock your screen when you double-tap Home."
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }
}
