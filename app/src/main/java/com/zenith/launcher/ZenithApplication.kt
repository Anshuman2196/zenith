package com.zenith.launcher
import android.app.Application
import com.zenith.launcher.di.AppContainer


/** Custom Application class - owns the single [AppContainer] instance for the process lifetime. */
class ZenithApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
