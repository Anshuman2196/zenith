package com.zenith.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zenith.launcher.ui.ViewModelFactory
import com.zenith.launcher.ui.navigation.LauncherNavHost
import com.zenith.launcher.ui.settings.SettingsViewModel
import com.zenith.launcher.ui.theme.ZenithLauncherTheme

/**
 * Single-activity entry point. Because this app is registered as HOME (see manifest), the
 * system starts this Activity on every "go to home screen" action - avoid heavy onCreate work.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // A home-screen launcher should never be "backed out of" - swallow system back here.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { /* intentionally no-op */ }
        })

        val container = (application as LauncherApplication).container
        val factory = ViewModelFactory(container)

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel(factory = factory)
            val isDarkMode by settingsViewModel.isDarkMode.collectAsState()
            val fontChoice by settingsViewModel.fontChoice.collectAsState()

            ZenithLauncherTheme(darkTheme = isDarkMode, fontChoice = fontChoice) {
                LauncherNavHost(viewModelFactory = factory)
            }
        }
    }
}
