package com.zenith.launcher.update
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.zenith.launcher.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL


@Serializable
data class GitHubRelease(
    @SerialName("tag_name") val tagName: String,
    val name: String? = null,
    val body: String? = null,
    val assets: List<GitHubReleaseAsset> = emptyList()
)

@Serializable
data class GitHubReleaseAsset(
    val name: String,
    @SerialName("browser_download_url") val browserDownloadUrl: String,
    val size: Long = 0L
)

data class UpdateInfo(
    val version: String,
    val title: String,
    val notes: String,
    val apkUrl: String,
    val apkSize: Long
)

sealed interface UpdateCheckResult {
    data object UpToDate : UpdateCheckResult
    data class Available(val update: UpdateInfo) : UpdateCheckResult
}

/** Checks and downloads signed APK releases published at the Zenith GitHub repository. */
object GitHubUpdateManager {
    private const val OWNER = "Anshuman2196"
    private const val REPOSITORY = "zenith"
    private const val RELEASES_URL =
        "https://api.github.com/repos/$OWNER/$REPOSITORY/releases?per_page=1"
    private const val APK_MIME = "application/vnd.android.package-archive"

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun checkForUpdate(): UpdateCheckResult = withContext(Dispatchers.IO) {
        val connection = (URL(RELEASES_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 15_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "Zenith-Android-Updater")
        }

        try {
            if (connection.responseCode !in 200..299) {
                throw IllegalStateException("GitHub returned HTTP ${connection.responseCode}")
            }
            val releases = connection.inputStream.bufferedReader().use {
                json.decodeFromString<List<GitHubRelease>>(it.readText())
            }
            val release = releases.firstOrNull() ?: return@withContext UpdateCheckResult.UpToDate
            val apk = release.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
                ?: throw IllegalStateException("The latest GitHub release does not contain an APK asset.")
            val latest = normalizeVersion(release.tagName)
            val current = normalizeVersion(BuildConfig.VERSION_NAME)
            if (compareVersions(latest, current) <= 0) {
                UpdateCheckResult.UpToDate
            } else {
                UpdateCheckResult.Available(
                    UpdateInfo(
                        version = release.tagName,
                        title = release.name?.takeIf { it.isNotBlank() } ?: release.tagName,
                        notes = release.body.orEmpty(),
                        apkUrl = apk.browserDownloadUrl,
                        apkSize = apk.size
                    )
                )
            }
        } finally {
            connection.disconnect()
        }
    }

    suspend fun downloadAndInstall(context: Context, update: UpdateInfo): Unit = withContext(Dispatchers.IO) {
        val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
        val apkFile = File(updatesDir, "zenith-${normalizeVersion(update.version)}.apk")
        if (apkFile.exists()) apkFile.delete()

        val connection = (URL(update.apkUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 60_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "Zenith-Android-Updater")
        }
        try {
            if (connection.responseCode !in 200..299) {
                throw IllegalStateException("Download failed with HTTP ${connection.responseCode}")
            }
            connection.inputStream.use { input ->
                apkFile.outputStream().use { output -> input.copyTo(output) }
            }
        } finally {
            connection.disconnect()
        }

        withContext(Dispatchers.Main) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                !context.packageManager.canRequestPackageInstalls()
            ) {
                val settingsIntent = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                )
                context.startActivity(settingsIntent)
                throw InstallPermissionRequiredException()
            }

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, APK_MIME)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(installIntent)
        }
    }

    fun formatSize(bytes: Long): String {
        if (bytes <= 0) return "APK"
        val mb = bytes / (1024.0 * 1024.0)
        return if (mb >= 1.0) "%.1f MB".format(mb) else "%.0f KB".format(bytes / 1024.0)
    }

    private fun normalizeVersion(value: String): List<Int> = value.trim().removePrefix("v").split(".").map { part ->
        part.takeWhile { it.isDigit() }.toIntOrNull() ?: 0
    }

    private fun compareVersions(left: List<Int>, right: List<Int>): Int {
        val size = maxOf(left.size, right.size)
        for (index in 0 until size) {
            val l = left.getOrElse(index) { 0 }
            val r = right.getOrElse(index) { 0 }
            if (l != r) return l.compareTo(r)
        }
        return 0
    }

    class InstallPermissionRequiredException : IllegalStateException()
}
