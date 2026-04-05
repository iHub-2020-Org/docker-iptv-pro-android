package com.iptvpro.tv.player

import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Surface
import com.iptvpro.tv.data.Config
import com.iptvpro.tv.data.model.Channel

class SafeMediaPlayer(
    private val surface: Surface,
    private val onError: () -> Unit,
    private val onPrepared: () -> Unit = {},
    private val onBuffering: ((Int) -> Unit)? = null
) {
    private var mediaPlayer: MediaPlayer? = null
    private var isPreparing = false
    private val timeoutHandler = Handler(Looper.getMainLooper())

    companion object {
        private const val TAG = "SafeMediaPlayer"
        private const val PREPARE_TIMEOUT_MS = 12000L  // 12s for slow TV networks
    }

    private val timeoutRunnable = Runnable {
        if (isPreparing) {
            Log.e(TAG, "Prepare timeout")
            stop()
            onError()
        }
    }

    fun play(channel: Channel) {
        try {
            stop()

            // ★★★ CRITICAL FIX: Route playback through the backend proxy.
            // The original channel.url uses IPv6 CDN which TV cannot access.
            // The proxy handles IPv6→IPv4 and rewrites TS segment URLs.
            // Proxy URL: http://SERVER/api/proxy/stream?url=ENCODED_ORIGINAL_URL
            val playUrl = buildProxyUrl(channel)
            Log.d(TAG, "Playing: $playUrl")

            mediaPlayer = MediaPlayer().apply {
                setSurface(surface)
                setDataSource(playUrl)

                setOnBufferingUpdateListener { _, percent ->
                    onBuffering?.invoke(percent)
                }

                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what extra=$extra")
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
                timeoutHandler.postDelayed(timeoutRunnable, PREPARE_TIMEOUT_MS)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Play failed", e)
            handleError()
        }
    }

    /**
     * Build the proxy URL for a channel.
     * The scanner backend proxy handles:
     *   - IPv6 CDN redirection (TV has no IPv6)
     *   - M3U8 segment URL rewriting
     *   - TS segment proxying
     */
    private fun buildProxyUrl(channel: Channel): String {
        val rawUrl = channel.url
        val encoded = Uri.encode(rawUrl)
        return "${Config.BASE_URL}${Config.Endpoints.PROXY_STREAM}?url=$encoded"
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
        try { mediaPlayer?.pause() } catch (_: Exception) {}
    }

    fun resume() {
        try { if (mediaPlayer?.isPlaying == false) mediaPlayer?.start() } catch (_: Exception) {}
    }

    val isPlaying: Boolean
        get() = mediaPlayer?.isPlaying == true

    private fun handleError() {
        stop()
        onError()
    }
}
