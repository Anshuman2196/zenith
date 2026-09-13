package com.zenith.launcher.data.repository

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.zenith.launcher.data.model.AppInfo
import com.zenith.launcher.data.model.IconPackInfo
import com.zenith.launcher.util.IconPackHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Source of truth for "what apps are installed" and "which icon packs are installed".
 * PackageManager is a blocking API, so every public function here runs on Dispatchers.IO.
 */
class AppRepository(private val context: Context) {

    private val packageManager: PackageManager get() = context.packageManager

    /** Every app with a launcher entry point, optionally re-skinned with [iconPackPackage]'s icons. */
    suspend fun getInstalledApps(iconPackPackage: String? = null): List<AppInfo> = withContext(Dispatchers.IO) {
        val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        val resolveInfos = packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        val iconPackHelper = iconPackPackage?.let { IconPackHelper(context, it) }

        resolveInfos
            .map { resolveInfo ->
                val pkg = resolveInfo.activityInfo.packageName
                val activityClass = resolveInfo.activityInfo.name
                val label = resolveInfo.loadLabel(packageManager).toString()

                // Prefer the icon-pack's themed icon; fall back to the app's own icon.
                val icon = iconPackHelper?.getIconFor(pkg, activityClass) ?: resolveInfo.loadIcon(packageManager)

                AppInfo(
                    packageName = pkg,
                    activityClassName = activityClass,
                    label = label,
                    icon = icon,
                    isSystemApp = isSystemApp(pkg)
                )
            }
            .distinctBy { it.packageName + it.activityClassName }
            .sortedBy { it.label.lowercase() }
    }

    private fun isSystemApp(packageName: String): Boolean = runCatching {
        val info = packageManager.getApplicationInfo(packageName, 0)
        (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0
    }.getOrDefault(false)

    /**
     * Finds installed icon packs by scanning for the standard ADW/Nova-compatible icon-pack
     * intent actions. See [IconPackHelper] for how icons are actually resolved from one.
     */
    suspend fun getInstalledIconPacks(): List<IconPackInfo> = withContext(Dispatchers.IO) {
        val iconPackIntentActions = listOf(
            "com.novalauncher.THEME",
            "org.adw.launcher.THEMES",
            "org.adw.launcher.icons.ACTION_PICK_ICON"
        )

        iconPackIntentActions
            .flatMap { action -> packageManager.queryIntentActivities(Intent(action), PackageManager.MATCH_ALL) }
            .map { it.activityInfo.packageName }
            .distinct()
            .mapNotNull { pkg ->
                runCatching {
                    val appInfo = packageManager.getApplicationInfo(pkg, 0)
                    IconPackInfo(packageName = pkg, label = packageManager.getApplicationLabel(appInfo).toString())
                }.getOrNull()
            }
    }

    /** Launches an app exactly as tapping its icon would. */
    fun launchApp(packageName: String, activityClassName: String) {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            component = ComponentName(packageName, activityClassName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        runCatching { context.startActivity(intent) }
    }
}
