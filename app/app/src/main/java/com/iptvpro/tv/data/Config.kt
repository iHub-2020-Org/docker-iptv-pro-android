package com.iptvpro.tv.data

import com.iptvpro.tv.BuildConfig

object Config {
    // Injected at build time via IPTV_BASE_URL env var or local.properties
    val BASE_URL: String = BuildConfig.BASE_URL
    
    object Endpoints {
        const val HEALTH = "/api/health"
        const val TEMPLATES = "/api/templates"
        const val SCAN_STREAM = "/api/scan/stream"
        const val SCAN_EXECUTE = "/api/scan/execute"
        const val SCAN_STOP = "/api/scan/stop"
        const val RESULTS = "/api/results"
        const val PLAYLIST_LIST = "/api/playlist/list"
        const val PLAYLIST_LOAD = "/api/playlist/load"
        const val PROXY_STREAM = "/api/proxy/stream"
    }
    
    // Default scan templates
    val DEFAULT_TEMPLATES = listOf(
        "http://ott.mobaibox.com/PLTV/4/224/322122{id}/index.m3u8",
        "http://ott.mobaibox.com/PLTV/4/224/322123{id}/index.m3u8",
        "http://ott.mobaibox.com/PLTV/4/224/322124{id}/index.m3u8",
        "http://ott.mobaibox.com/PLTV/4/224/322125{id}/index.m3u8",
        "http://ott.mobaibox.com/PLTV/4/224/322126{id}/index.m3u8"
    )
}
