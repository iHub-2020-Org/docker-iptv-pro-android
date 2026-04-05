package com.iptvpro.tv.safety

import android.app.ActivityManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Process
import android.util.Log
import kotlin.system.exitProcess

/**
 * Custom uncaught exception handler that logs crashes and saves state.
 */
class CrashHandler(
    private val context: Context,
    private val defaultHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    companion object {
        private const val TAG = "CrashHandler"
        private const val PREFS_NAME = "safety_prefs"
        private const val KEY_LAST_CRASHED = "last_crashed"

        /**
         * Initialize the crash handler for the application.
         */
        @JvmStatic
        fun init(context: Context) {
            val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler(CrashHandler(context.applicationContext, defaultHandler))
        }
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        // Log the crash
        Log.e(TAG, "Uncaught exception in thread ${thread.name}", throwable)
        
        try {
            // Save crashed flag to SharedPreferences
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_LAST_CRASHED, true).apply()
            
            // Flush to ensure it's written before process dies
            prefs.edit().commit()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save crash state", e)
        }
        
        // Call the default handler if available
        defaultHandler?.uncaughtException(thread, throwable) ?: run {
            // Gracefully kill the process if no default handler
            killProcess()
        }
    }
    
    /**
     * Kills the process gracefully.
     */
    private fun killProcess() {
        Process.killProcess(Process.myPid())
        exitProcess(1)
    }
}
