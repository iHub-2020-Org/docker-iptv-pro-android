package com.iptvpro.tv.data.api

import android.os.Handler
import android.os.Looper
import com.iptvpro.tv.data.Config
import java.net.HttpURLConnection
import java.net.URL

object ApiClient {
    private const val CONNECT_TIMEOUT = 5000
    private const val READ_TIMEOUT = 10000
    
    fun getSync(path: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL(Config.BASE_URL + path)
            conn = url.openConnection() as HttpURLConnection
            conn.apply {
                connectTimeout = CONNECT_TIMEOUT
                readTimeout = READ_TIMEOUT
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
            }
            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else null
        } catch (e: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }
    
    fun postAsync(path: String, body: String, callback: (String?) -> Unit) {
        Thread {
            val result = postSync(path, body)
            Handler(Looper.getMainLooper()).post { callback(result) }
        }.start()
    }
    
    private fun postSync(path: String, body: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL(Config.BASE_URL + path)
            conn = url.openConnection() as HttpURLConnection
            conn.apply {
                connectTimeout = CONNECT_TIMEOUT
                readTimeout = READ_TIMEOUT
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
            conn.outputStream.use { it.write(body.toByteArray()) }
            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else null
        } catch (e: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }
    
    fun isServerAvailable(): Boolean = getSync(Config.Endpoints.HEALTH) != null
}
