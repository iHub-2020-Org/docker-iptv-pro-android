package com.iptvpro.tv

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.iptvpro.tv.data.Config
import com.iptvpro.tv.data.api.ApiClient

class SettingsActivity : Activity() {

    private lateinit var etHost: EditText
    private lateinit var etPort: EditText
    private lateinit var tvPreview: TextView
    private lateinit var tvStatus: TextView
    private lateinit var btnTest: TextView
    private lateinit var btnSave: TextView

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        etHost    = findViewById(R.id.etServerHost)
        etPort    = findViewById(R.id.etServerPort)
        tvPreview = findViewById(R.id.tvUrlPreview)
        tvStatus  = findViewById(R.id.tvConnStatus)
        btnTest   = findViewById(R.id.btnTest)
        btnSave   = findViewById(R.id.btnSave)

        // Pre-fill current config
        populateCurrentConfig()

        // Live URL preview
        val watcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { updatePreview() }
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
        }
        etHost.addTextChangedListener(watcher)
        etPort.addTextChangedListener(watcher)

        btnTest.setOnClickListener { testConnection() }
        btnSave.setOnClickListener { saveAndExit() }

        // D-pad: enter on buttons
        btnTest.setOnKeyListener { _, keyCode, _ ->
            if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
                testConnection(); true
            } else false
        }
        btnSave.setOnKeyListener { _, keyCode, _ ->
            if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
                saveAndExit(); true
            } else false
        }

        // Focus host field first
        etHost.requestFocus()
    }

    private fun populateCurrentConfig() {
        val url = Config.BASE_URL  // already loaded from prefs in Application.onCreate
        // Parse host and port from current BASE_URL
        val regex = Regex("""^https?://([^:/]+)(?::(\d+))?""")
        val match = regex.find(url)
        if (match != null) {
            etHost.setText(match.groupValues[1])
            etPort.setText(match.groupValues[2].ifEmpty { "5950" })
        } else {
            etHost.setText("")
            etPort.setText("5950")
        }
        updatePreview()
    }

    private fun buildUrl(): String {
        val host = etHost.text.toString().trim()
        val port = etPort.text.toString().trim().ifEmpty { "5950" }
        if (host.isEmpty()) return ""
        return "http://$host:$port"
    }

    private fun updatePreview() {
        val url = buildUrl()
        tvPreview.text = if (url.isEmpty()) "" else url
        tvStatus.text = ""
    }

    private fun testConnection() {
        val url = buildUrl()
        if (url.isEmpty()) {
            tvStatus.text = "❌ 请先填写服务器地址"
            tvStatus.setTextColor(0xFFFF6B6B.toInt())
            return
        }
        tvStatus.text = "⏳ 连接测试中..."
        tvStatus.setTextColor(0xFF8B949E.toInt())
        btnTest.isEnabled = false

        Thread {
            // Temporarily override BASE_URL for the test
            val savedUrl = Config.BASE_URL
            Config.saveBaseUrl(this, url)
            val ok = ApiClient.isServerAvailable()
            // Restore if user hasn't saved yet
            if (!ok) Config.saveBaseUrl(this, savedUrl)

            mainHandler.post {
                btnTest.isEnabled = true
                if (ok) {
                    tvStatus.text = "✅ 连接成功！服务器正常运行"
                    tvStatus.setTextColor(0xFF3FB950.toInt())
                } else {
                    tvStatus.text = "❌ 无法连接 — 请检查IP、端口和网络"
                    tvStatus.setTextColor(0xFFFF6B6B.toInt())
                    // Restore original
                    Config.saveBaseUrl(this, savedUrl)
                }
            }
        }.start()
    }

    private fun saveAndExit() {
        val url = buildUrl()
        if (url.isEmpty()) {
            Toast.makeText(this, "请输入服务器地址", Toast.LENGTH_SHORT).show()
            return
        }
        Config.saveBaseUrl(this, url)
        Toast.makeText(this, "✅ 已保存: $url", Toast.LENGTH_SHORT).show()
        setResult(RESULT_OK)
        finish()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            setResult(RESULT_CANCELED)
            finish()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}
