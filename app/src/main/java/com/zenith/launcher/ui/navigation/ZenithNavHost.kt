package com.zenith.launcher.ui.navigation
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.zenith.launcher.ui.home.HomeScreen
import com.zenith.launcher.ui.home.HomeViewModel
import com.zenith.launcher.ui.onboarding.OnboardingScreen
import com.zenith.launcher.ui.settings.SettingsScreen
import com.zenith.launcher.ui.settings.SettingsViewModel


object ZenithRoutes {
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val ONBOARDING_PREVIEW = "onboarding_preview"
}

@Composable
fun ZenithNavHost(viewModelFactory: ViewModelProvider.Factory) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = ZenithRoutes.HOME) {
        composable(ZenithRoutes.HOME) {
            val homeViewModel: HomeViewModel = viewModel(factory = viewModelFactory)
            HomeScreen(
                viewModel = homeViewModel,
                onOpenSettings = { navController.navigate(ZenithRoutes.SETTINGS) }
            )
        }
        composable(ZenithRoutes.SETTINGS) {
            val settingsViewModel: SettingsViewModel = viewModel(factory = viewModelFactory)
            SettingsScreen(
                viewModel = settingsViewModel,
                onBack = { navController.popBackStack() },
                onPreviewOnboarding = { navController.navigate(ZenithRoutes.ONBOARDING_PREVIEW) }
            )
        }
        composable(ZenithRoutes.ONBOARDING_PREVIEW) {
            val settingsViewModel: SettingsViewModel = viewModel(factory = viewModelFactory)
            OnboardingScreen(
                settingsViewModel = settingsViewModel,
                onFinished = { navController.popBackStack() }
            )
        }
    }
}
