package com.zenith.launcher

import android.os.Bundle
import android.os.Build
import android.Manifest
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zenith.launcher.ui.ViewModelFactory
import com.zenith.launcher.ui.navigation.ZenithNavHost
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

        // A home-screen launcher should never be "backed out of" - swallow system back here.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { /* intentionally no-op */ }
        })

        // Once per cold start rather than on every onResume, so this doesn't nag every single
        // time the user returns to Home if they dismiss the system prompt without acting on it.
        DefaultHomeHelper.requestIfNeeded(this, defaultHomeRoleRequest)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionRequest.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val container = (application as ZenithApplication).container
        val factory = ViewModelFactory(container)

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel(factory = factory)
            val isDarkMode by settingsViewModel.isDarkMode.collectAsState()
            val fontChoice by settingsViewModel.fontChoice.collectAsState()
            val customFontPath by settingsViewModel.customFontPath.collectAsState()

            ZenithStudyHelperTheme(darkTheme = isDarkMode, fontChoice = fontChoice, customFontPath = customFontPath) {
                ZenithNavHost(viewModelFactory = factory)
            }
        }
    }
}
