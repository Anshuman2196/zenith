package com.zenith.launcher.di

import android.content.Context
import com.zenith.launcher.data.local.PreferencesManager
import com.zenith.launcher.data.repository.AppRepository
import com.zenith.launcher.data.repository.SettingsRepository

/**
 * Minimal manual DI container (no Hilt/Dagger, to keep Gradle/CI simple). Created once in
 * [com.zenith.launcher.LauncherApplication] and handed to ViewModels via
 * [com.zenith.launcher.ui.ViewModelFactory].
 */
class AppContainer(context: Context) {
    private val preferencesManager = PreferencesManager(context.applicationContext)

    val appRepository = AppRepository(context.applicationContext)
    val settingsRepository = SettingsRepository(preferencesManager)
}
