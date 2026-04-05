package com.iptvpro.tv

import android.app.Activity
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ListView
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
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : Activity(), SurfaceHolder.Callback {

    companion object {
        private const val TAG = "MainActivity"
        const val MODE_PLAY = 0
        const val MODE_LIST = 1
        const val MODE_SCAN = 2
        private const val INFO_BAR_TIMEOUT_MS = 4000L
        private const val REQ_SETTINGS = 1001
    }

    private var currentMode = MODE_PLAY
    private var currentChannelIndex = 0
    private val channels = mutableListOf<Channel>()
    private var isScanning = false
    private var scanFoundCount = 0

    // Views
    private lateinit var surfaceView: SurfaceView
    private lateinit var playLayout: FrameLayout
    private lateinit var listLayout: LinearLayout
    private lateinit var scanLayout: LinearLayout
    private lateinit var infoBar: LinearLayout
    private lateinit var bufferingLayout: LinearLayout
    private lateinit var noChannelsLayout: LinearLayout
    private lateinit var tvChannelName: TextView
    private lateinit var tvChannelRes: TextView
    private lateinit var tvChannelIndex: TextView
    private lateinit var tvChannelCount: TextView
    private lateinit var tvScanStatus: TextView
    private lateinit var tvScanFound: TextView
    private lateinit var tvNoChannels: TextView
    private lateinit var listChannels: ListView
    private lateinit var channelAdapter: ArrayAdapter<String>

    private var player: SafeMediaPlayer? = null
    private var surface: Surface? = null
    private var sseClient: SseClient? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val infoBarHideRunnable = Runnable { infoBar.visibility = View.GONE }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!SafetyCheck.checkBeforeLaunch(this)) {
            Toast.makeText(this, R.string.safe_mode, Toast.LENGTH_LONG).show()
        }
        setContentView(R.layout.activity_main)
        initViews()

        // ★ First-launch check: if server not configured, go to Settings immediately
        if (!Config.isConfigured(this)) {
            openSettings()
        } else {
            loadChannels()
        }
    }

    private fun initViews() {
        surfaceView      = findViewById(R.id.surfaceView)
        playLayout       = findViewById(R.id.playLayout)
        listLayout       = findViewById(R.id.listLayout)
        scanLayout       = findViewById(R.id.scanLayout)
        infoBar          = findViewById(R.id.infoBar)
        bufferingLayout  = findViewById(R.id.bufferingLayout)
        noChannelsLayout = findViewById(R.id.noChannelsLayout)
        tvChannelName    = findViewById(R.id.tvChannelName)
        tvChannelRes     = findViewById(R.id.tvChannelRes)
        tvChannelIndex   = findViewById(R.id.tvChannelIndex)
        tvChannelCount   = findViewById(R.id.tvChannelCount)
        tvScanStatus     = findViewById(R.id.tvScanStatus)
        tvScanFound      = findViewById(R.id.tvScanFound)
        tvNoChannels     = findViewById(R.id.tvNoChannelsHint)
        listChannels     = findViewById(R.id.listChannels)

        surfaceView.holder.addCallback(this)

        channelAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        listChannels.adapter = channelAdapter
        listChannels.setOnItemClickListener { _, _, position, _ ->
            currentChannelIndex = position
            PlayListCache.saveLastChannelIndex(this, currentChannelIndex)
            switchMode(MODE_PLAY)
            playCurrentChannel()
        }
    }

    // ─── Settings ────────────────────────────────────────────────────────────

    private fun openSettings() {
        startActivityForResult(Intent(this, SettingsActivity::class.java), REQ_SETTINGS)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_SETTINGS) {
            // Reload channels after server URL may have changed
            channels.clear()
            player?.stop(); player = null
            loadChannels()
            Toast.makeText(this, "服务器: ${Config.BASE_URL}", Toast.LENGTH_SHORT).show()
        }
    }

    // ─── Channel Loading ─────────────────────────────────────────────────────

    private fun loadChannels() {
        val cached = PlayListCache.load(this)
        if (cached.isNotEmpty()) {
            channels.addAll(cached)
            currentChannelIndex = PlayListCache.getLastChannelIndex(this)
                .coerceIn(0, channels.size - 1)
            refreshChannelUI()
        }
        Thread { fetchFromServer() }.start()
    }

    private fun fetchFromServer() {
        try {
            val resp = ApiClient.getSync(Config.Endpoints.RESULTS) ?: return
            val arr  = JSONObject(resp).optJSONArray("results") ?: return
            if (arr.length() == 0 || arr.length() <= channels.size) return
            val fresh = (0 until arr.length()).map { Channel.fromJson(arr.getJSONObject(it)) }
            mainHandler.post {
                channels.clear(); channels.addAll(fresh)
                PlayListCache.save(this, channels)
                currentChannelIndex = currentChannelIndex.coerceIn(0, maxOf(0, channels.size - 1))
                refreshChannelUI()
                if (surface != null && player == null && channels.isNotEmpty()) playCurrentChannel()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Server fetch: ${e.message}")
            mainHandler.post {
                if (channels.isEmpty()) {
                    tvNoChannels.text = getString(R.string.settings_not_configured)
                        .takeIf { !Config.isConfigured(this) }
                        ?: getString(R.string.server_hint)
                    noChannelsLayout.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun refreshChannelUI() {
        val names = channels.map { "${it.name}  ${it.resolution.takeIf { r -> r != "未知" } ?: ""}" }
        channelAdapter.clear(); channelAdapter.addAll(names)
        channelAdapter.notifyDataSetChanged()
        tvChannelCount.text = "${channels.size} 个频道"
        noChannelsLayout.visibility = if (channels.isEmpty()) View.VISIBLE else View.GONE
    }

    // ─── Playback ────────────────────────────────────────────────────────────

    private fun playCurrentChannel() {
        val sfc = surface ?: return
        if (channels.isEmpty()) { noChannelsLayout.visibility = View.VISIBLE; return }
        noChannelsLayout.visibility = View.GONE
        player?.stop()
        bufferingLayout.visibility = View.VISIBLE
        val ch = channels[currentChannelIndex]
        player = SafeMediaPlayer(
            surface    = sfc,
            onError    = { mainHandler.post { onPlayError() } },
            onPrepared = { mainHandler.post { bufferingLayout.visibility = View.GONE } },
            onBuffering = { pct -> mainHandler.post {
                bufferingLayout.visibility = if (pct < 95) View.VISIBLE else View.GONE
            }}
        )
        player!!.play(ch)
        showInfoBar(ch)
    }

    private fun showInfoBar(ch: Channel) {
        tvChannelIndex.text = "${currentChannelIndex + 1}/${channels.size}"
        tvChannelName.text  = ch.name
        tvChannelRes.text   = ch.resolution.takeIf { it != "未知" } ?: ""
        infoBar.visibility  = View.VISIBLE
        mainHandler.removeCallbacks(infoBarHideRunnable)
        mainHandler.postDelayed(infoBarHideRunnable, INFO_BAR_TIMEOUT_MS)
    }

    private fun onPlayError() {
        bufferingLayout.visibility = View.GONE
        Toast.makeText(this, R.string.play_error, Toast.LENGTH_SHORT).show()
        mainHandler.postDelayed({ playNext() }, 1500)
    }

    private fun playNext() {
        if (channels.isEmpty()) return
        currentChannelIndex = (currentChannelIndex + 1) % channels.size
        PlayListCache.saveLastChannelIndex(this, currentChannelIndex)
        playCurrentChannel()
    }

    private fun playPrevious() {
        if (channels.isEmpty()) return
        currentChannelIndex = (currentChannelIndex - 1 + channels.size) % channels.size
        PlayListCache.saveLastChannelIndex(this, currentChannelIndex)
        playCurrentChannel()
    }

    // ─── Surface ─────────────────────────────────────────────────────────────

    override fun surfaceCreated(holder: SurfaceHolder) {
        surface = holder.surface
        if (channels.isNotEmpty()) playCurrentChannel()
    }
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        player?.stop(); player = null; surface = null
    }

    // ─── Mode ────────────────────────────────────────────────────────────────

    private fun switchMode(mode: Int) {
        currentMode = mode
        playLayout.visibility  = if (mode == MODE_PLAY) View.VISIBLE else View.GONE
        listLayout.visibility  = if (mode == MODE_LIST) View.VISIBLE else View.GONE
        scanLayout.visibility  = if (mode == MODE_SCAN) View.VISIBLE else View.GONE
        when (mode) {
            MODE_LIST -> {
                listChannels.requestFocus()
                if (currentChannelIndex < channelAdapter.count)
                    listChannels.setSelection(currentChannelIndex)
            }
            MODE_PLAY -> if (channels.isNotEmpty()) showInfoBar(channels[currentChannelIndex])
        }
    }

    // ─── Keys ────────────────────────────────────────────────────────────────

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean = when (currentMode) {
        MODE_PLAY -> handlePlayKeys(keyCode)
        MODE_LIST -> handleListKeys(keyCode)
        MODE_SCAN -> handleScanKeys(keyCode)
        else      -> super.onKeyDown(keyCode, event)
    }

    private fun handlePlayKeys(keyCode: Int): Boolean = when (keyCode) {
        KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_PAGE_UP    -> { playPrevious(); true }
        KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_PAGE_DOWN -> { playNext();     true }
        KeyEvent.KEYCODE_DPAD_UP   -> { adjustVolume(1);  true }
        KeyEvent.KEYCODE_DPAD_DOWN -> { adjustVolume(-1); true }
        KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_DPAD_CENTER -> {
            if (channels.isNotEmpty()) switchMode(MODE_LIST); true
        }
        KeyEvent.KEYCODE_MENU -> { switchMode(MODE_SCAN); true }
        KeyEvent.KEYCODE_INFO -> {
            if (infoBar.visibility == View.VISIBLE) infoBar.visibility = View.GONE
            else if (channels.isNotEmpty()) showInfoBar(channels[currentChannelIndex])
            true
        }
        // ★ Press 0 → open Settings from any mode
        KeyEvent.KEYCODE_0 -> { openSettings(); true }
        else -> super.onKeyDown(keyCode, null)
    }

    private fun handleListKeys(keyCode: Int): Boolean = when (keyCode) {
        KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_DPAD_LEFT -> { switchMode(MODE_PLAY); true }
        KeyEvent.KEYCODE_MENU  -> { switchMode(MODE_SCAN); true }
        KeyEvent.KEYCODE_0     -> { openSettings(); true }
        else -> super.onKeyDown(keyCode, null)
    }

    private fun handleScanKeys(keyCode: Int): Boolean = when (keyCode) {
        KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_DPAD_LEFT -> { switchMode(MODE_PLAY); true }
        KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_DPAD_CENTER -> {
            if (isScanning) stopScan() else startScan(); true
        }
        KeyEvent.KEYCODE_0 -> { openSettings(); true }
        else -> super.onKeyDown(keyCode, null)
    }

    // ─── Volume ──────────────────────────────────────────────────────────────

    private fun adjustVolume(delta: Int) {
        (getSystemService(AUDIO_SERVICE) as AudioManager).adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            if (delta > 0) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER,
            AudioManager.FLAG_SHOW_UI
        )
    }

    // ─── Scan ────────────────────────────────────────────────────────────────

    private fun startScan() {
        isScanning = true; scanFoundCount = 0
        tvScanStatus.text = getString(R.string.scanning)
        tvScanFound.text  = ""
        channels.clear(); refreshChannelUI()
        val config = ScanConfig(Config.DEFAULT_TEMPLATES.first(), "id", "0000-0100", 30, 8)
        ApiClient.postAsync(Config.Endpoints.SCAN_EXECUTE, config.toJson().toString()) { resp ->
            if (resp != null) connectScanSse()
            else { isScanning = false; tvScanStatus.text = "连接服务器失败 — 按 0 检查设置" }
        }
    }

    private fun stopScan() {
        ApiClient.postAsync(Config.Endpoints.SCAN_STOP, "{}") {}
        sseClient?.disconnect(); isScanning = false
        tvScanStatus.text = getString(R.string.scan_ready)
    }

    private fun connectScanSse() {
        sseClient?.disconnect()
        sseClient = SseClient { event, data ->
            when (event) {
                "channel_found" -> { scanFoundCount++; val ch = Channel.fromJson(data)
                    mainHandler.post { channels.add(ch); refreshChannelUI()
                        tvScanFound.text = getString(R.string.channels_found, scanFoundCount) }
                }
                "scan_complete" -> mainHandler.post {
                    isScanning = false; tvScanStatus.text = getString(R.string.scan_complete)
                    PlayListCache.save(this, channels)
                    if (channels.isNotEmpty()) mainHandler.postDelayed({
                        switchMode(MODE_PLAY); currentChannelIndex = 0; playCurrentChannel()
                    }, 1000)
                }
                "error" -> mainHandler.post { isScanning = false; tvScanStatus.text = "扫描出错" }
            }
        }
        sseClient?.connect(Config.Endpoints.SCAN_STREAM)
    }

    // ─── Lifecycle ───────────────────────────────────────────────────────────

    override fun onPause() {
        super.onPause(); player?.pause()
        mainHandler.removeCallbacks(infoBarHideRunnable)
    }
    override fun onResume() { super.onResume(); player?.resume() }
    override fun onDestroy() {
        super.onDestroy(); sseClient?.disconnect(); player?.stop()
        mainHandler.removeCallbacksAndMessages(null)
        surfaceView.holder.removeCallback(this)
    }
}
