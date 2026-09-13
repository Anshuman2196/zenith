package com.zenith.launcher.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.zenith.launcher.ui.home.HomeScreen
import com.zenith.launcher.ui.home.HomeViewModel
import com.zenith.launcher.ui.settings.SettingsScreen
import com.zenith.launcher.ui.settings.SettingsViewModel

object LauncherRoutes {
    const val HOME = "home"
    const val SETTINGS = "settings"
}

@Composable
fun LauncherNavHost(viewModelFactory: ViewModelProvider.Factory) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = LauncherRoutes.HOME) {
        composable(LauncherRoutes.HOME) {
            val homeViewModel: HomeViewModel = viewModel(factory = viewModelFactory)
            HomeScreen(
                viewModel = homeViewModel,
                onOpenSettings = { navController.navigate(LauncherRoutes.SETTINGS) }
            )
        }
        composable(LauncherRoutes.SETTINGS) {
            val settingsViewModel: SettingsViewModel = viewModel(factory = viewModelFactory)
            SettingsScreen(
                viewModel = settingsViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
