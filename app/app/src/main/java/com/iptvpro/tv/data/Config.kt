package com.iptvpro.tv.data

import android.content.Context
import com.iptvpro.tv.BuildConfig

object Config {

    // ── SharedPreferences key ──────────────────────────────────────────────
    private const val PREFS_NAME  = "iptv_settings"
    private const val KEY_BASE_URL = "base_url"

    /**
     * Default URL: injected at build time (env var IPTV_BASE_URL or fallback).
     * Used when the user has not yet configured a custom server.
     */
    val DEFAULT_BASE_URL: String = BuildConfig.BASE_URL

    /**
     * Runtime BASE_URL — call init(context) once in Application.onCreate().
     * After that, BASE_URL always returns the user-configured value.
     */
    var BASE_URL: String = BuildConfig.BASE_URL
        private set

    /** Call once from Application.onCreate() to load persisted URL. */
    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        BASE_URL = prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
    }

    /** Persist a new base URL and update the in-memory value immediately. */
    fun saveBaseUrl(context: Context, url: String) {
        val cleaned = url.trimEnd('/')
        BASE_URL = cleaned
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_BASE_URL, cleaned)
            .apply()
    }

    /** True if user has explicitly set a server address (not using default). */
    fun isConfigured(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.contains(KEY_BASE_URL)
    }

    // ── API endpoints ──────────────────────────────────────────────────────
    object Endpoints {
        const val HEALTH        = "/api/health"
        const val TEMPLATES     = "/api/templates"
        const val SCAN_STREAM   = "/api/scan/stream"
        const val SCAN_EXECUTE  = "/api/scan/execute"
        const val SCAN_STOP     = "/api/scan/stop"
        const val RESULTS       = "/api/results"
        const val PROXY_STREAM  = "/api/proxy/stream"
    }

    // ── Default IPTV scan templates ────────────────────────────────────────
    val DEFAULT_TEMPLATES = listOf(
        "http://ott.mobaibox.com/PLTV/4/224/322122{id}/index.m3u8",
        "http://ott.mobaibox.com/PLTV/4/224/322123{id}/index.m3u8",
        "http://ott.mobaibox.com/PLTV/4/224/322124{id}/index.m3u8",
        "http://ott.mobaibox.com/PLTV/4/224/322125{id}/index.m3u8",
        "http://ott.mobaibox.com/PLTV/4/224/322126{id}/index.m3u8"
    )
}
