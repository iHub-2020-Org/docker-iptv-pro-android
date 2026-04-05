package com.iptvpro.tv

import android.app.Application
import com.iptvpro.tv.data.Config
import com.iptvpro.tv.safety.CrashHandler

class SafeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashHandler.init(this)
        // Load user-configured server URL from SharedPreferences
        Config.init(this)
    }
}
