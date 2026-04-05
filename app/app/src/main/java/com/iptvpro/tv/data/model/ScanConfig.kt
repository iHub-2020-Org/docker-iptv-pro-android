package com.iptvpro.tv.data.model

import org.json.JSONObject

data class ScanConfig(
    val template: String,
    val variable: String,
    val range: String,
    val threads: Int = 50,
    val timeout: Int = 5
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("template", template)
            put("variable", variable)
            put("range", range)
            put("threads", threads)
            put("timeout", timeout)
        }
    }
}
