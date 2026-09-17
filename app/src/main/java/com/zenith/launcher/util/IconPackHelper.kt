package com.zenith.launcher.util
import androidx.compose.runtime.getValue
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory


/**
 * Resolves themed icons from a third-party icon pack (e.g. Lawnicons, Whicons, or packs
 * exported from Icon Pack Studio).
 *
 * Icon packs ship an `appfilter.xml` mapping "ComponentInfo{package/activity}" -> a drawable
 * name in the pack's own resources - the same convention used by ADW/Nova-compatible icon
 * packs, so this covers most packs on the Play Store. Most packs place this file at
 * `res/xml/appfilter.xml` (a compiled Android XML resource), but packs exported from Icon Pack
 * Studio place it at `res/raw/appfilter.xml` (a plain-text XML file instead), so both locations
 * are checked.
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

        // Standard ADW/Nova-compatible packs: compiled XML resource at res/xml/appfilter.xml.
        val xmlResId = resources.getIdentifier("appfilter", "xml", iconPackPackageName)
        if (xmlResId != 0) {
            val parsed = runCatching { parseItems(resources.getXml(xmlResId)) }.getOrNull()
            if (!parsed.isNullOrEmpty()) return parsed
        }

        // Icon Pack Studio-exported packs: plain-text XML file at res/raw/appfilter.xml.
        val rawResId = resources.getIdentifier("appfilter", "raw", iconPackPackageName)
        if (rawResId != 0) {
            val parsed = runCatching {
                resources.openRawResource(rawResId).use { stream ->
                    val parser = XmlPullParserFactory.newInstance().newPullParser()
                    parser.setInput(stream, null)
                    parseItems(parser)
                }
            }.getOrNull()
            if (!parsed.isNullOrEmpty()) return parsed
        }

        return emptyMap()
    }

    /** Reads `<item component="..." drawable="..."/>` entries from an already-positioned parser. */
    private fun parseItems(parser: XmlPullParser): Map<String, String> {
        val map = mutableMapOf<String, String>()
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.name == "item") {
                val component = parser.getAttributeValue(null, "component")
                val drawable = parser.getAttributeValue(null, "drawable")
                if (component != null && drawable != null) map[component] = drawable
            }
            eventType = parser.next()
        }
        return map
    }
}
