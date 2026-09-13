package com.zenith.launcher.util

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import org.xmlpull.v1.XmlPullParser

/**
 * Resolves themed icons from a third-party icon pack (e.g. Lawnicons, Whicons).
 *
 * Icon packs ship a `res/xml/appfilter.xml` mapping
 * "ComponentInfo{package/activity}" -> a drawable name in the pack's own resources -
 * the same convention used by ADW/Nova-compatible icon packs, so this covers most packs
 * on the Play Store.
 */
class IconPackHelper(context: Context, private val iconPackPackageName: String) {

    private val packageManager: PackageManager = context.packageManager

    private val iconPackResources by lazy {
        runCatching { packageManager.getResourcesForApplication(iconPackPackageName) }.getOrNull()
    }

    /** componentName string -> drawable resource name, parsed once and cached. */
    private val componentToDrawableName: Map<String, String> by lazy { parseAppFilter() }

    /** Returns the icon-pack's replacement icon for this app, or null if it doesn't theme it. */
    fun getIconFor(packageName: String, activityClassName: String): Drawable? {
        val resources = iconPackResources ?: return null
        val key = "ComponentInfo{$packageName/$activityClassName}"
        val drawableName = componentToDrawableName[key] ?: return null
        val resId = resources.getIdentifier(drawableName, "drawable", iconPackPackageName)
        if (resId == 0) return null
        return runCatching { resources.getDrawable(resId, null) }.getOrNull()
    }

    private fun parseAppFilter(): Map<String, String> {
        val resources = iconPackResources ?: return emptyMap()
        val xmlResId = resources.getIdentifier("appfilter", "xml", iconPackPackageName)
        if (xmlResId == 0) return emptyMap()

        val map = mutableMapOf<String, String>()
        runCatching {
            val parser = resources.getXml(xmlResId)
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "item") {
                    val component = parser.getAttributeValue(null, "component")
                    val drawable = parser.getAttributeValue(null, "drawable")
                    if (component != null && drawable != null) map[component] = drawable
                }
                eventType = parser.next()
            }
        }
        return map
    }
}
