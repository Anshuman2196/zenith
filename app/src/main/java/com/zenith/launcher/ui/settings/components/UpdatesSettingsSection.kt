package com.zenith.launcher.ui.settings.components
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.zenith.launcher.BuildConfig
import com.zenith.launcher.update.GitHubUpdateManager
import com.zenith.launcher.update.UpdateCheckResult
import com.zenith.launcher.update.UpdateInfo
import kotlinx.coroutines.launch


@Composable
fun UpdatesSettingsSection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var checking by remember { mutableStateOf(false) }
    var installing by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var update by remember { mutableStateOf<UpdateInfo?>(null) }

    SettingsSectionCard(title = "Updates") {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Installed version ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyMedium)
            Text(
                update?.let { "Version ${it.version} is available. ${GitHubUpdateManager.formatSize(it.apkSize)}." }
                    ?: (message ?: "Check GitHub for a newer Zenith release."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            update?.let { available ->
                if (available.notes.isNotBlank()) {
                    Text(
                        available.notes.trim().take(500),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Button(
                onClick = {
                    scope.launch {
                        if (update == null) {
                            checking = true
                            message = null
                            runCatching { GitHubUpdateManager.checkForUpdate() }
                                .onSuccess { result ->
                                    when (result) {
                                        UpdateCheckResult.UpToDate -> message = "You're up to date."
                                        is UpdateCheckResult.Available -> update = result.update
                                    }
                                }
                                .onFailure { message = it.message ?: "Couldn't check for updates." }
                            checking = false
                        } else {
                            installing = true
                            runCatching { GitHubUpdateManager.downloadAndInstall(context, update!!) }
                                .onFailure {
                                    message = if (it is GitHubUpdateManager.InstallPermissionRequiredException) {
                                        "Allow Zenith to install updates, then tap Update now again."
                                    } else {
                                        it.message ?: "Couldn't download the update."
                                    }
                                }
                            installing = false
                        }
                    }
                },
                enabled = !checking && !installing,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (checking || installing) {
                    CircularProgressIndicator(strokeWidth = 2.dp)
                } else {
                    Text(if (update == null) "Check for updates" else "Update now")
                }
            }
        }
    }
}
