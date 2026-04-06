package com.iptvpro.tv.data

import android.content.Context
import com.iptvpro.tv.BuildConfig

object Config {
    private const val PREFS_NAME   = "iptv_settings"
    private const val KEY_BASE_URL = "base_url"
    private const val KEY_PLAYLIST = "last_playlist"

    val DEFAULT_BASE_URL: String = BuildConfig.BASE_URL
    var BASE_URL: String = BuildConfig.BASE_URL
        private set

    fun init(context: Context) {
        val p = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        BASE_URL = p.getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
    }

    fun saveBaseUrl(context: Context, url: String) {
        BASE_URL = url.trimEnd('/')
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_BASE_URL, BASE_URL).apply()
    }

    fun saveLastPlaylist(context: Context, filename: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_PLAYLIST, filename).apply()
    }

    fun getLastPlaylist(context: Context): String? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PLAYLIST, null)

    fun isConfigured(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .contains(KEY_BASE_URL)

    object Endpoints {
        const val HEALTH          = "/api/health"
        const val SCAN_STREAM     = "/api/scan/stream"
        const val SCAN_EXECUTE    = "/api/scan/execute"
        const val SCAN_STOP       = "/api/scan/stop"
        const val RESULTS         = "/api/results"        // 内存结果（扫描后临时）
        const val PLAYLIST_LIST   = "/api/playlist/list"  // 列出持久化列表
        const val PLAYLIST_LOAD   = "/api/playlist/load"  // 加载持久化列表
        const val PROXY_STREAM    = "/api/proxy/stream"
    }

    val DEFAULT_TEMPLATES = listOf(
        "http://ott.mobaibox.com/PLTV/4/224/322122{id}/index.m3u8",
        "http://ott.mobaibox.com/PLTV/4/224/322123{id}/index.m3u8",
        "http://ott.mobaibox.com/PLTV/4/224/322124{id}/index.m3u8",
        "http://ott.mobaibox.com/PLTV/4/224/322125{id}/index.m3u8",
        "http://ott.mobaibox.com/PLTV/4/224/322126{id}/index.m3u8"
    )
}
