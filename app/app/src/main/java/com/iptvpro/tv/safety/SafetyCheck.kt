package com.iptvpro.tv.safety

import android.content.Context
import android.os.Build
import android.util.Log

object SafetyCheck {
    private const val TAG = "SafetyCheck"
    private const val MIN_FREE_MEMORY_MB = 50
    private const val MIN_SDK_VERSION = Build.VERSION_CODES.KITKAT
    
    fun checkBeforeLaunch(context: Context): Boolean {
        // 检查内存
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory() / 1024 / 1024
        val freeMemory = runtime.freeMemory() / 1024 / 1024
        
        Log.d(TAG, "Max: ${maxMemory}MB, Free: ${freeMemory}MB")
        
        if (freeMemory < MIN_FREE_MEMORY_MB) {
            Log.w(TAG, "Memory too low: ${freeMemory}MB")
            return false
        }
        
        // 检查上次崩溃
        val prefs = context.getSharedPreferences("safe", Context.MODE_PRIVATE)
        if (prefs.getBoolean("last_crashed", false)) {
            Log.w(TAG, "Last launch crashed")
            prefs.edit().putBoolean("last_crashed", false).apply()
            return false
        }
        
        // 检查版本
        if (Build.VERSION.SDK_INT < MIN_SDK_VERSION) {
            Log.w(TAG, "SDK too low: ${Build.VERSION.SDK_INT}")
            return false
        }
        
        return true
    }
}
