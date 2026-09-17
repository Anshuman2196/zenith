package com.zenith.launcher.util
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity


/**
 * Being set as the default launcher used to mean walking the user to Settings > Apps > Default
 * apps > Home app themselves. On API 29+, [RoleManager] can show that exact system prompt
 * directly - one tap, no manual navigation - so this always prefers that path and only falls
 * back to opening Settings on older Android versions.
 */
object DefaultHomeHelper {

    fun isDefaultHomeApp(context: Context): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolved = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolved?.activityInfo?.packageName == context.packageName
    }

    /** Shows the "set as default Home app" prompt if - and only if - it isn't already default. */
    fun requestIfNeeded(activity: ComponentActivity, homeRoleRequest: androidx.activity.result.ActivityResultLauncher<Intent>) {
        if (isDefaultHomeApp(activity)) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = activity.getSystemService(Context.ROLE_SERVICE) as? RoleManager
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME) && !roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
                runCatching { homeRoleRequest.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)) }
                return
            }
        }

        // Pre-Q fallback: no one-tap system prompt exists, so this is the closest thing -
        // Android's own "choose Home app" screen, one level up from digging through Settings.
        runCatching {
            activity.startActivity(Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
