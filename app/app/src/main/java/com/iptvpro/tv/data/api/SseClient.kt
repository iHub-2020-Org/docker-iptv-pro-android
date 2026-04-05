package com.iptvpro.tv.data.api

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.iptvpro.tv.data.Config
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class SseClient(private val onEvent: (String, JSONObject) -> Unit) {
    private var thread: Thread? = null
    private var isRunning = false
    
    fun connect(path: String) {
        disconnect()
        isRunning = true
        
        thread = Thread {
            var conn: HttpURLConnection? = null
            try {
                val url = URL(Config.BASE_URL + path)
                conn = url.openConnection() as HttpURLConnection
                conn.apply {
                    connectTimeout = 5000
                    readTimeout = 0
                    setRequestProperty("Accept", "text/event-stream")
                    setRequestProperty("Cache-Control", "no-cache")
                }
                
                conn.inputStream.bufferedReader().useLines { lines ->
                    var eventType = "message"
                    lines.forEach { line ->
                        if (!isRunning) return@forEach
                        when {
                            line.startsWith("event:") -> eventType = line.substring(6).trim()
                            line.startsWith("data:") -> {
                                val data = line.substring(5).trim()
                                try {
                                    val json = JSONObject(data)
                                    Handler(Looper.getMainLooper()).post {
                                        onEvent(eventType, json)
                                    }
                                } catch (_: Exception) {}
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("SSE", "Connection error", e)
            } finally {
                conn?.disconnect()
            }
        }.apply { start() }
    }
    
    fun disconnect() {
        isRunning = false
        thread?.interrupt()
        thread = null
    }
}
