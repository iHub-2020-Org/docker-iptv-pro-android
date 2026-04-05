package com.iptvpro.tv.data.model

import org.json.JSONObject

data class Channel(
    val id: String,
    val name: String,
    val url: String,
    val realUrl: String? = null,
    val resolution: String = "未知",
    val status: String = "active"
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("name", name)
            put("url", url)
            put("real_url", realUrl)
            put("resolution", resolution)
            put("status", status)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): Channel {
            return Channel(
                id = json.getString("id"),
                name = json.getString("name"),
                url = json.getString("url"),
                realUrl = json.optString("real_url").takeIf { it.isNotEmpty() },
                resolution = json.optString("resolution", "未知"),
                status = json.optString("status", "active")
            )
        }
    }
}
