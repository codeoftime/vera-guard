package com.vera.guard

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import org.osmdroid.config.Configuration

@HiltAndroidApp
class VeraApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // osmdroid needs a user agent and a cache dir inside app storage
        Configuration.getInstance().userAgentValue = packageName
        Configuration.getInstance().osmdroidBasePath = cacheDir
        Configuration.getInstance().osmdroidTileCache = java.io.File(cacheDir, "tiles")
    }
}
