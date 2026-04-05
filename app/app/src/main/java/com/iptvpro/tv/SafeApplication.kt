package com.iptvpro.tv

import android.app.Application
import android.content.Context
import android.util.Log
import kotlin.system.exitProcess

class SafeApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Set global exception handler
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("CRASH", "Uncaught exception in ${thread.name}", throwable)
            
            // Save crash flag
            getSharedPreferences("safe", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("last_crashed", true)
                .apply()
            
            // Exit gracefully
            exitProcess(1)
        }
    }
}
