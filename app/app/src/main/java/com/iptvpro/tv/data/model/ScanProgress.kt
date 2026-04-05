package com.iptvpro.tv.data.model

data class ScanProgress(
    val checked: Int,
    val total: Int,
    val found: Int,
    val channel: Channel? = null
)
