package com.iptvpro.tv

import android.app.Application
import com.iptvpro.tv.safety.CrashHandler

class SafeApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        // Use the dedicated CrashHandler instead of inline lambda
        CrashHandler.init(this)
    }
}
