package com.iptvpro.tv

import android.app.Activity
import android.media.AudioManager
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.GridView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import com.iptvpro.tv.data.Config
import com.iptvpro.tv.data.api.ApiClient
import com.iptvpro.tv.data.api.SseClient
import com.iptvpro.tv.data.cache.PlayListCache
import com.iptvpro.tv.data.model.Channel
import com.iptvpro.tv.data.model.ScanConfig
import com.iptvpro.tv.player.SafeMediaPlayer
import com.iptvpro.tv.safety.SafetyCheck
import org.json.JSONObject

class MainActivity : Activity(), SurfaceHolder.Callback {
    
    companion object {
        const val MODE_PLAY = 0
        const val MODE_LIST = 1
        const val MODE_SCAN = 2
    }
    
    private var currentMode = MODE_PLAY
    private var currentChannelIndex = 0
    private var channels = mutableListOf<Channel>()
    
    private lateinit var surfaceView: SurfaceView
    private lateinit var playLayout: LinearLayout
    private lateinit var listLayout: LinearLayout
    private lateinit var scanLayout: LinearLayout
    private lateinit var infoBar: LinearLayout
    private lateinit var tvChannelName: TextView
    private lateinit var tvChannelRes: TextView
    
    private var player: SafeMediaPlayer? = null
    private var surface: Surface? = null
    private var sseClient: SseClient? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (!SafetyCheck.checkBeforeLaunch(this)) {
            Toast.makeText(this, "安全模式", Toast.LENGTH_LONG).show()
        }
        
        setContentView(R.layout.activity_main)
        
        initViews()
        initPlayer()
        loadChannels()
    }
    
    private fun initViews() {
        surfaceView = findViewById(R.id.surfaceView)
        surfaceView.holder.addCallback(this)
        
        playLayout = findViewById(R.id.playLayout)
        listLayout = findViewById(R.id.listLayout)
        scanLayout = findViewById(R.id.scanLayout)
        infoBar = findViewById(R.id.infoBar)
        tvChannelName = findViewById(R.id.tvChannelName)
        tvChannelRes = findViewById(R.id.tvChannelRes)
        
        // 隐藏非播放界面
        listLayout.visibility = View.GONE
        scanLayout.visibility = View.GONE
    }
    
    private fun initPlayer() {
        surface?.let {
            player = SafeMediaPlayer(it, 
                onError = { runOnUiThread { onPlayError() } },
                onPrepared = { runOnUiThread { onPlayPrepared() } }
            )
        }
    }
    
    private fun loadChannels() {
        channels = PlayListCache.load(this).toMutableList()
        if (channels.isNotEmpty()) {
            currentChannelIndex = PlayListCache.getLastChannelIndex(this)
                .coerceIn(0, channels.size - 1)
            playCurrentChannel()
        } else {
            showNoChannelsMessage()
        }
    }
    
    private fun playCurrentChannel() {
        if (channels.isEmpty()) return
        
        val channel = channels[currentChannelIndex]
        surface?.let { surface ->
            if (player == null) {
                player = SafeMediaPlayer(surface,
                    onError = { runOnUiThread { onPlayError() } },
                    onPrepared = { runOnUiThread { onPlayPrepared() } }
                )
            }
            player?.play(channel)
            updateInfoBar(channel)
        }
    }
    
    private fun updateInfoBar(channel: Channel) {
        tvChannelName.text = channel.name
        tvChannelRes.text = channel.resolution
    }
    
    private fun onPlayError() {
        Toast.makeText(this, "播放失败", Toast.LENGTH_SHORT).show()
    }
    
    private fun onPlayPrepared() {
        // 播放成功
    }
    
    private fun showNoChannelsMessage() {
        Toast.makeText(this, "按菜单键配置扫描", Toast.LENGTH_LONG).show()
    }
    
    override fun surfaceCreated(holder: SurfaceHolder) {
        surface = holder.surface
        if (player == null) {
            player = SafeMediaPlayer(surface!!,
                onError = { runOnUiThread { onPlayError() } },
                onPrepared = { runOnUiThread { onPlayPrepared() } }
            )
        }
        if (channels.isNotEmpty()) {
            playCurrentChannel()
        }
    }
    
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        surface = null
    }
    
    private fun switchMode(mode: Int) {
        currentMode = mode
        playLayout.visibility = if (mode == MODE_PLAY) View.VISIBLE else View.GONE
        listLayout.visibility = if (mode == MODE_LIST) View.VISIBLE else View.GONE
        scanLayout.visibility = if (mode == MODE_SCAN) View.VISIBLE else View.GONE
        infoBar.visibility = if (mode == MODE_PLAY) View.VISIBLE else View.GONE
    }
    
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (currentMode) {
            MODE_PLAY -> handlePlayKeys(keyCode)
            MODE_LIST -> handleListKeys(keyCode)
            MODE_SCAN -> handleScanKeys(keyCode)
            else -> super.onKeyDown(keyCode, event)
        }
    }
    
    private fun handlePlayKeys(keyCode: Int): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                playPrevious()
                true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                playNext()
                true
            }
            KeyEvent.KEYCODE_DPAD_UP -> {
                adjustVolume(1)
                true
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                adjustVolume(-1)
                true
            }
            KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_DPAD_CENTER -> {
                switchMode(MODE_LIST)
                true
            }
            KeyEvent.KEYCODE_MENU -> {
                switchMode(MODE_SCAN)
                true
            }
            else -> super.onKeyDown(keyCode, null)
        }
    }
    
    private fun handleListKeys(keyCode: Int): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_BACK -> {
                switchMode(MODE_PLAY)
                true
            }
            else -> super.onKeyDown(keyCode, null)
        }
    }
    
    private fun handleScanKeys(keyCode: Int): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_BACK -> {
                switchMode(MODE_PLAY)
                true
            }
            KeyEvent.KEYCODE_ENTER -> {
                startScan()
                true
            }
            else -> super.onKeyDown(keyCode, null)
        }
    }
    
    private fun playPrevious() {
        if (channels.isEmpty()) return
        currentChannelIndex = (currentChannelIndex - 1 + channels.size) % channels.size
        PlayListCache.saveLastChannelIndex(this, currentChannelIndex)
        playCurrentChannel()
    }
    
    private fun playNext() {
        if (channels.isEmpty()) return
        currentChannelIndex = (currentChannelIndex + 1) % channels.size
        PlayListCache.saveLastChannelIndex(this, currentChannelIndex)
        playCurrentChannel()
    }
    
    private fun adjustVolume(delta: Int) {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            if (delta > 0) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER,
            AudioManager.FLAG_SHOW_UI
        )
    }
    
    private fun startScan() {
        val config = ScanConfig(
            template = "http://ott.mobaibox.com/PLTV/4/224/322122{id}/index.m3u8",
            variable = "id",
            range = "0000-0100",
            threads = 50,
            timeout = 5
        )
        
        ApiClient.postAsync(Config.Endpoints.SCAN_EXECUTE, config.toJson().toString()) { response ->
            if (response != null) {
                connectSse()
            } else {
                Toast.makeText(this, "扫描启动失败", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun connectSse() {
        sseClient?.disconnect()
        sseClient = SseClient { event, data ->
            when (event) {
                "channel_found" -> {
                    val channel = Channel.fromJson(data)
                    channels.add(channel)
                }
                "scan_complete" -> {
                    PlayListCache.save(this, channels)
                    runOnUiThread {
                        switchMode(MODE_PLAY)
                        playCurrentChannel()
                    }
                }
            }
        }
        sseClient?.connect(Config.Endpoints.SCAN_STREAM)
    }
    
    override fun onPause() {
        super.onPause()
        player?.pause()
    }
    
    override fun onResume() {
        super.onResume()
        player?.resume()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        sseClient?.disconnect()
        player?.stop()
        surfaceView.holder.removeCallback(this)
    }
}
