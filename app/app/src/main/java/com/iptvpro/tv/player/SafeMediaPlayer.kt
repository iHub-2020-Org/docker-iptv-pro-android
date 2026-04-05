package com.iptvpro.tv.player

import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Surface
import com.iptvpro.tv.data.model.Channel

class SafeMediaPlayer(
    private val surface: Surface,
    private val onError: () -> Unit,
    private val onPrepared: () -> Unit = {}
) {
    private var mediaPlayer: MediaPlayer? = null
    private var isPreparing = false
    private val timeoutHandler = Handler(Looper.getMainLooper())
    
    private val timeoutRunnable = Runnable {
        if (isPreparing) {
            Log.e("PLAYER", "Prepare timeout")
            stop()
            onError()
        }
    }
    
    fun play(channel: Channel) {
        try {
            stop()
            
            mediaPlayer = MediaPlayer().apply {
                setSurface(surface)
                val playUrl = channel.realUrl ?: channel.url
                setDataSource(playUrl)
                
                setOnErrorListener { _, what, extra ->
                    Log.e("PLAYER", "Error: $what, $extra")
                    handleError()
                    true
                }
                
                setOnPreparedListener {
                    isPreparing = false
                    timeoutHandler.removeCallbacks(timeoutRunnable)
                    start()
                    onPrepared()
                }
                
                isPreparing = true
                prepareAsync()
                timeoutHandler.postDelayed(timeoutRunnable, 5000)
            }
        } catch (e: Exception) {
            Log.e("PLAYER", "Play failed", e)
            handleError()
        }
    }
    
    fun stop() {
        isPreparing = false
        timeoutHandler.removeCallbacks(timeoutRunnable)
        
        mediaPlayer?.let {
            try {
                if (it.isPlaying) it.stop()
                it.release()
            } catch (_: Exception) {}
            mediaPlayer = null
        }
    }
    
    fun pause() {
        try {
            mediaPlayer?.pause()
        } catch (_: Exception) {}
    }
    
    fun resume() {
        try {
            mediaPlayer?.start()
        } catch (_: Exception) {}
    }
    
    fun setVolume(volume: Float) {
        try {
            mediaPlayer?.setVolume(volume, volume)
        } catch (_: Exception) {}
    }
    
    private fun handleError() {
        stop()
        onError()
    }
}
