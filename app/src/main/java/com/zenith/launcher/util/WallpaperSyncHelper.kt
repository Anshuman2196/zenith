package com.zenith.launcher.util

import android.app.WallpaperManager
import android.content.Context
import android.net.Uri
import android.os.Build

/**
 * Keeps the phone's actual wallpaper (Home and, optionally, Lock screen) in sync with whatever
 * background photo is chosen in Settings > Appearance, so the launcher's own background isn't
 * the only place that photo shows up.
 */
object WallpaperSyncHelper {

    /**
     * Applies [uri] as the system wallpaper. When [alsoLockScreen] is true (API 24+) it's set for
     * both Home and Lock screen in one call; otherwise only Home is touched, leaving whatever
     * Lock screen wallpaper the user already had.
     */
    fun apply(context: Context, uri: Uri, alsoLockScreen: Boolean) {
        // 1080x1920 comfortably covers every phone's actual display density after Android's own
        // wallpaper scaling, without decoding at full camera resolution - see BitmapUtils.
        val bitmap = BitmapUtils.decodeSampledBitmapFromUri(context, uri, 1080, 1920) ?: return
        val wallpaperManager = WallpaperManager.getInstance(context)

        runCatching {
            if (alsoLockScreen && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)
            } else {
                wallpaperManager.setBitmap(bitmap)
            }
        }
    }
}
