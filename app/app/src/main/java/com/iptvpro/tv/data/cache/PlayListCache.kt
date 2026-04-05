package com.iptvpro.tv.data.cache

import android.content.Context
import com.iptvpro.tv.data.model.Channel
import org.json.JSONArray
import org.json.JSONObject

object PlayListCache {
    private const val PREFS_NAME = "iptv_cache"
    private const val KEY_PLAYLIST = "playlist"
    private const val KEY_LAST_CHANNEL = "last_channel"
    
    fun save(context: Context, channels: List<Channel>) {
        val jsonArray = JSONArray()
        channels.forEach { channel ->
            jsonArray.put(channel.toJson())
        }
        
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PLAYLIST, jsonArray.toString())
            .apply()
    }
    
    fun load(context: Context): List<Channel> {
        val jsonString = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PLAYLIST, null)
            
        return if (jsonString.isNullOrEmpty()) {
            emptyList()
        } else {
            try {
                val jsonArray = JSONArray(jsonString)
                val channels = mutableListOf<Channel>()
                for (i in 0 until jsonArray.length()) {
                    channels.add(Channel.fromJson(jsonArray.getJSONObject(i)))
                }
                channels
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
    
    fun clear(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_PLAYLIST)
            .remove(KEY_LAST_CHANNEL)
            .apply()
    }
    
    fun saveLastChannelIndex(context: Context, index: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_LAST_CHANNEL, index)
            .apply()
    }
    
    fun getLastChannelIndex(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_LAST_CHANNEL, 0)
    }
}
