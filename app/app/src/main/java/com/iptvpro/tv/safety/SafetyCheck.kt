package com.iptvpro.tv.safety

import android.content.Context
import android.os.Build
import android.util.Log

object SafetyCheck {
    private const val TAG = "SafetyCheck"
    private const val MIN_FREE_MEMORY_MB = 50
    private const val MIN_SDK_VERSION = Build.VERSION_CODES.LOLLIPOP

    // Use the same prefs name as CrashHandler for consistency
    internal const val PREFS_NAME = "safety_prefs"
    internal const val KEY_LAST_CRASHED = "last_crashed"
    
    fun checkBeforeLaunch(context: Context): Boolean {
        // Check available memory
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory() / 1024 / 1024
        val freeMemory = runtime.freeMemory() / 1024 / 1024
        
        Log.d(TAG, "Max: ${maxMemory}MB, Free: ${freeMemory}MB")
        
        if (freeMemory < MIN_FREE_MEMORY_MB) {
            Log.w(TAG, "Memory too low: ${freeMemory}MB")
            return false
        }
        
        // Check if last launch crashed (written by CrashHandler)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_LAST_CRASHED, false)) {
            Log.w(TAG, "Last launch crashed, running in safe mode")
            prefs.edit().putBoolean(KEY_LAST_CRASHED, false).apply()
            return false
        }
        
        // Check API level
        if (Build.VERSION.SDK_INT < MIN_SDK_VERSION) {
            Log.w(TAG, "SDK too low: ${Build.VERSION.SDK_INT}")
            return false
        }
        
        return true
    }
}
