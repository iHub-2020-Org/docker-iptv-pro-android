package com.iptvpro.tv.data.api

import android.os.Handler
import android.os.Looper
import com.iptvpro.tv.data.Config
import java.net.HttpURLConnection
import java.net.URL

object ApiClient {
    private const val CONNECT_TIMEOUT = 5000
    private const val READ_TIMEOUT    = 10000

    fun getSync(path: String): String? = request("GET", Config.BASE_URL + path, null)

    fun postAsync(path: String, body: String, callback: (String?) -> Unit) {
        Thread {
            val result = request("POST", Config.BASE_URL + path, body)
            Handler(Looper.getMainLooper()).post { callback(result) }
        }.start()
    }

    private fun request(method: String, fullUrl: String, body: String?): String? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(fullUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = CONNECT_TIMEOUT
                readTimeout    = READ_TIMEOUT
                requestMethod  = method
                if (body != null) {
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    outputStream.use { it.write(body.toByteArray()) }
                }
                setRequestProperty("Accept", "application/json")
            }
            if (conn.responseCode in 200..299)
                conn.inputStream.bufferedReader().use { it.readText() }
            else null
        } catch (e: Exception) { null }
        finally { conn?.disconnect() }
    }

    fun isServerAvailable(): Boolean = getSync(Config.Endpoints.HEALTH) != null
}
