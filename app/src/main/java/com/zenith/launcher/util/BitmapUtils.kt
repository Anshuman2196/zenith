package com.zenith.launcher.util
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri


/**
 * A user's background photo is very often a full camera-resolution image (12MP+ - tens of
 * megabytes once decoded as ARGB_8888). Decoding it at full size every place it's shown (Home,
 * the Settings preview thumbnail) is the single biggest avoidable memory
 * cost in this app, especially on low-RAM devices - this is the fix: decode straight to roughly
 * the target size using [BitmapFactory.Options.inSampleSize], and use the half-memory RGB_565
 * config since a background photo doesn't need an alpha channel or 8-bit-per-channel precision.
 */
object BitmapUtils {

    fun decodeSampledBitmapFromUri(context: Context, uri: Uri, reqWidth: Int, reqHeight: Int): Bitmap? = runCatching {
        val resolver = context.contentResolver
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, boundsOptions) }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(boundsOptions, reqWidth, reqHeight)
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, decodeOptions) }
    }.getOrNull()

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
