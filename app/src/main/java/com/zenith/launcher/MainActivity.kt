package com.zenith.launcher
import androidx.compose.runtime.getValue
import android.os.Bundle
import android.os.Build
import android.Manifest
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zenith.launcher.ui.ViewModelFactory
import com.zenith.launcher.ui.navigation.ZenithNavHost
import com.zenith.launcher.ui.onboarding.OnboardingScreen
import com.zenith.launcher.ui.settings.SettingsViewModel
import com.zenith.launcher.ui.theme.ZenithStudyHelperTheme
import com.zenith.launcher.util.DefaultHomeHelper


/**
 * Single-activity entry point. Because this app is registered as HOME (see manifest), the
 * system starts this Activity on every "go to home screen" action - avoid heavy onCreate work.
 */
class MainActivity : ComponentActivity() {

    // Must be registered unconditionally before STARTED, per the Activity Result API's rules -
    // actually prompting only happens once, from onCreate, via DefaultHomeHelper.
    private val defaultHomeRoleRequest =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { /* no-op either way */ }

    private val notificationPermissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* timer still works in-app if declined */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as ZenithApplication).container
        val factory = ViewModelFactory(container)

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel(factory = factory)
            val isDarkMode by settingsViewModel.isDarkMode.collectAsState()
            val fontChoice by settingsViewModel.fontChoice.collectAsState()
            val customFontPath by settingsViewModel.customFontPath.collectAsState()
            val onboardingCompleted by settingsViewModel.onboardingCompleted.collectAsState()

            LaunchedEffect(onboardingCompleted) {
                if (onboardingCompleted) {
                    // Ask for launcher/notification setup only after the user has seen Zenith's
                    // introduction, so system prompts do not become the first impression.
                    DefaultHomeHelper.requestIfNeeded(this@MainActivity, defaultHomeRoleRequest)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionRequest.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            ZenithStudyHelperTheme(darkTheme = isDarkMode, fontChoice = fontChoice, customFontPath = customFontPath) {
                if (onboardingCompleted) {
                    ZenithNavHost(viewModelFactory = factory)
                } else {
                    OnboardingScreen(
                        settingsViewModel = settingsViewModel,
                        onFinished = { settingsViewModel.completeOnboarding() }
                    )
                }
            }
        }
    }
}
