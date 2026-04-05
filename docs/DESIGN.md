# IPTV Pro Android - 架构设计文档

## 1. 系统架构

### 1.1 整体架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                      IPTV Pro Android                           │
│                        (TV客户端)                               │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │                   MainActivity                          │  │
│  │              (单一Activity，三状态切换)                 │  │
│  ├─────────────────────────────────────────────────────────┤  │
│  │                                                         │  │
│  │   ┌──────────┐   ┌──────────┐   ┌──────────┐          │  │
│  │   │ MODE_PLAY│   │MODE_LIST │   │ MODE_SCAN│          │  │
│  │   │ 全屏播放  │   │ 频道列表  │   │ 扫描配置  │          │  │
│  │   └──────────┘   └──────────┘   └──────────┘          │  │
│  └─────────────────────────────────────────────────────────┘  │
│                                                                 │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │ SafeMedia    │  │ ChannelList  │  │ ScanManager  │          │
│  │ Player       │  │ Fragment     │  │              │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
│         │                 │                  │                   │
│  ┌──────┴──────────────────┴──────────────────┴─────────────┐    │
│  │                     Data Layer                         │    │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌────────┐ │    │
│  │  │ ApiClient│  │SseClient │  │PlayList  │  │Config  │ │    │
│  │  │ (HTTP)   │  │ (SSE)    │  │Cache     │  │Manager │ │    │
│  │  └──────────┘  └──────────┘  └──────────┘  └────────┘ │    │
│  └────────────────────────────────────────────────────────┘    │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │                   Protection Layer                        │  │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌────────┐  │  │
│  │  │ Memory   │  │ Crash    │  │ SafeMode │  │ANR     │  │  │
│  │  │ Monitor  │  │ Handler  │  │ Manager  │  │Guard    │  │  │
│  │  └──────────┘  └──────────┘  └──────────┘  └────────┘  │  │
│  └─────────────────────────────────────────────────────────┘  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ HTTP
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    iptv-scanner-pro                             │
│                    (后端服务)                                     │
│              http://192.168.9.158:5950                          │
└─────────────────────────────────────────────────────────────────┘
```

### 1.2 模块职责

| 模块 | 职责 | 关键类 |
|------|------|--------|
| **UI Layer** | 界面展示与交互 | `MainActivity`, `ChannelListFragment` |
| **Player Layer** | 视频播放控制 | `SafeMediaPlayer` |
| **Data Layer** | 数据获取与存储 | `ApiClient`, `SseClient`, `PlayListCache` |
| **Scan Layer** | 扫描流程管理 | `ScanManager` |
| **Protection Layer** | 系统安全防护 | `CrashHandler`, `MemoryMonitor` |

## 2. 技术选型

### 2.1 零依赖策略

为保证APK体积最小化，本项目**不使用任何第三方库**：

| 功能需求 | 原生方案 | 避免的第三方库 |
|----------|----------|-------------|
| 网络请求 | `HttpURLConnection` | OkHttp, Retrofit |
| JSON解析 | `org.json` | Gson, Jackson |
| 图像加载 | 系统`Bitmap` | Glide, Picasso |
| 异步任务 | `AsyncTask`, `HandlerThread` | RxJava, Kotlin协程 |
| 依赖注入 | 手动工厂模式 | Dagger, Hilt, Koin |
| 播放器 | `MediaPlayer` | ExoPlayer, IJKPlayer |

### 2.2 系统API使用

```kotlin
// 网络
java.net.HttpURLConnection
java.net.URL

// JSON
org.json.JSONObject
org.json.JSONArray

// 媒体
android.media.MediaPlayer
android.view.SurfaceView

// 存储
android.content.SharedPreferences

// 线程
android.os.AsyncTask
android.os.HandlerThread
android.os.Handler

// UI
android.app.Activity
android.app.Fragment
```

## 3. 数据模型

### 3.1 Channel (频道)

```kotlin
data class Channel(
    val id: String,           // 频道ID
    val name: String,         // 频道名称
    val url: String,          // M3U8地址
    val realUrl: String? = null,  // 代理地址
    val resolution: String = "未知", // 分辨率
    val status: String = "active"   // 状态
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
                realUrl = json.optString("real_url", null),
                resolution = json.optString("resolution", "未知"),
                status = json.optString("status", "active")
            )
        }
    }
}
```

### 3.2 ScanConfig (扫描配置)

```kotlin
data class ScanConfig(
    val template: String,     // URL模板
    val variable: String,     // 变量名
    val range: String,        // 范围 (0000-9999)
    val threads: Int = 50,    // 并发线程
    val timeout: Int = 5      // 超时秒数
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
```

### 3.3 ScanProgress (扫描进度)

```kotlin
data class ScanProgress(
    val checked: Int,   // 已检查数量
    val total: Int,     // 总数量
    val found: Int,     // 找到频道数
    val channel: Channel? = null  // 新发现的频道
)
```

## 4. 核心类设计

### 4.1 MainActivity (主控制器)

```kotlin
class MainActivity : Activity() {
    // 三种模式
    companion object {
        const val MODE_PLAY = 0   // 全屏播放
        const val MODE_LIST = 1   // 频道列表
        const val MODE_SCAN = 2   // 扫描配置
    }
    
    private var currentMode = MODE_PLAY
    private lateinit var player: SafeMediaPlayer
    private lateinit var channelList: ChannelListFragment
    private lateinit var scanManager: ScanManager
    
    // 状态管理
    private var currentChannelIndex = 0
    private var channels = mutableListOf<Channel>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 安全检查
        if (!SafetyCheck.checkBeforeLaunch(this)) {
            showSafeModeDialog()
            return
        }
        
        // 初始化
        initComponents()
        
        // 加载播放列表
        loadPlayList()
        
        // 切换到播放模式
        switchMode(MODE_PLAY)
    }
    
    // 按键处理
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (currentMode) {
            MODE_PLAY -> handlePlayModeKey(keyCode)
            MODE_LIST -> handleListModeKey(keyCode)
            MODE_SCAN -> handleScanModeKey(keyCode)
            else -> super.onKeyDown(keyCode, event)
        }
    }
}
```

### 4.2 SafeMediaPlayer (安全播放器)

```kotlin
class SafeMediaPlayer(
    private val surfaceView: SurfaceView,
    private val onError: () -> Unit
) {
    private var mediaPlayer: MediaPlayer? = null
    private var isPreparing = false
    
    // 播放超时处理
    private val timeoutHandler = Handler(Looper.getMainLooper())
    private val timeoutRunnable = Runnable {
        if (isPreparing) {
            stop()
            onError()
        }
    }
    
    fun play(channel: Channel) {
        try {
            // 清理旧播放器
            stop()
            
            // 创建新播放器
            mediaPlayer = MediaPlayer().apply {
                setSurface(surfaceView.holder.surface)
                
                // 使用代理URL
                val playUrl = channel.realUrl ?: channel.url
                setDataSource(playUrl)
                
                // 错误监听
                setOnErrorListener { _, what, extra ->
                    handleError(what, extra)
                    true
                }
                
                // 准备完成
                setOnPreparedListener {
                    isPreparing = false
                    timeoutHandler.removeCallbacks(timeoutRunnable)
                    start()
                }
                
                // 异步准备
                isPreparing = true
                prepareAsync()
                
                // 5秒超时
                timeoutHandler.postDelayed(timeoutRunnable, 5000)
            }
        } catch (e: Exception) {
            handleError(-1, -1)
        }
    }
    
    fun stop() {
        isPreparing = false
        timeoutHandler.removeCallbacks(timeoutRunnable)
        
        mediaPlayer?.let {
            try {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            } catch (e: Exception) {
                // 忽略释放异常
            }
            mediaPlayer = null
        }
    }
    
    private fun handleError(what: Int, extra: Int) {
        stop()
        onError()
    }
}
```

### 4.3 ApiClient (HTTP客户端)

```kotlin
object ApiClient {
    private const val BASE_URL = "http://192.168.9.158:5950"
    private const val CONNECT_TIMEOUT = 5000
    private const val READ_TIMEOUT = 10000
    
    // API端点
    object Endpoints {
        const val HEALTH = "/api/health"
        const val TEMPLATES = "/api/templates"
        const val SCAN_EXECUTE = "/api/scan/execute"
        const val SCAN_STOP = "/api/scan/stop"
        const val RESULTS = "/api/results"
        const val PLAYLIST_LIST = "/api/playlist/list"
        const val PLAYLIST_LOAD = "/api/playlist/load"
        const val PROXY_STREAM = "/api/proxy/stream"
    }
    
    // GET请求（同步）
    fun getSync(path: String): String? {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(BASE_URL + path)
            connection = url.openConnection() as HttpURLConnection
            connection.apply {
                connectTimeout = CONNECT_TIMEOUT
                readTimeout = READ_TIMEOUT
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
            }
            
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }
    
    // POST请求（异步）
    fun postAsync(
        path: String,
        jsonBody: String,
        callback: (String?) -> Unit
    ) {
        Thread {
            val result = postSync(path, jsonBody)
            Handler(Looper.getMainLooper()).post {
                callback(result)
            }
        }.start()
    }
    
    private fun postSync(path: String, jsonBody: String): String? {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(BASE_URL + path)
            connection = url.openConnection() as HttpURLConnection
            connection.apply {
                connectTimeout = CONNECT_TIMEOUT
                readTimeout = READ_TIMEOUT
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
            
            connection.outputStream.use { os ->
                os.write(jsonBody.toByteArray())
            }
            
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }
    
    // 检查服务器可用
    fun isServerAvailable(): Boolean {
        return getSync(Endpoints.HEALTH) != null
    }
}
```

### 4.4 SseClient (SSE推送客户端)

```kotlin
class SseClient(
    private val onEvent: (String, JSONObject) -> Unit
) {
    private var thread: Thread? = null
    private var isRunning = false
    
    fun connect(path: String) {
        disconnect()
        isRunning = true
        
        thread = Thread {
            var connection: HttpURLConnection? = null
            try {
                val url = URL(ApiClient.BASE_URL + path)
                connection = url.openConnection() as HttpURLConnection
                connection.apply {
                    connectTimeout = 5000
                    readTimeout = 0  // SSE需要长连接
                    setRequestProperty("Accept", "text/event-stream")
                    setRequestProperty("Cache-Control", "no-cache")
                }
                
                connection.inputStream.bufferedReader().useLines { lines ->
                    var eventType = "message"
                    
                    lines.forEach { line ->
                        if (!isRunning) return@forEach
                        
                        when {
                            line.startsWith("event:") -> {
                                eventType = line.substring(6).trim()
                            }
                            line.startsWith("data:") -> {
                                val data = line.substring(5).trim()
                                try {
                                    val json = JSONObject(data)
                                    Handler(Looper.getMainLooper()).post {
                                        onEvent(eventType, json)
                                    }
                                } catch (e: Exception) {
                                    // 解析失败忽略
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // 连接断开
            } finally {
                connection?.disconnect()
            }
        }.apply { start() }
    }
    
    fun disconnect() {
        isRunning = false
        thread?.interrupt()
        thread = null
    }
}
```

## 5. 安全设计

详见 [SECURITY.md](./SECURITY.md)

### 5.1 防护层级

```
Layer 1: 启动前检查
    └─ 内存检测、版本检查

Layer 2: 异常捕获
    └─ 全局UncaughtExceptionHandler

Layer 3: 运行时监控
    └─ 内存使用监控、ANR防护

Layer 4: 组件保护
    └─ 播放器try-catch、网络超时

Layer 5: 安全模式
    └─ 崩溃后自动进入降级模式

Layer 6: 数据保护
    └─ 定期备份、异常恢复
```

## 6. UI设计

### 6.1 界面状态

| 状态 | 布局 | 元素 |
|------|------|------|
| **MODE_PLAY** | `activity_main.xml` | SurfaceView + 底部信息栏 |
| **MODE_LIST** | `fragment_channels.xml` | 网格列表 + 分类侧边栏 |
| **MODE_SCAN** | `fragment_scan.xml` | 配置表单 + 进度显示 |

### 6.2 暗色主题

```xml
<!-- colors.xml -->
<color name="tv_background">#0D1117</color>
<color name="tv_surface">#161B22</color>
<color name="tv_card_focus">#1C2128</color>
<color name="tv_accent">#3B82F6</color>
<color name="tv_text_primary">#E6EDF3</color>
<color name="tv_text_secondary">#8B949E</color>
```

## 7. 数据流

### 7.1 播放流程

```
用户打开App
    ↓
加载本地播放列表缓存
    ↓
有缓存? ──→ 是 ──→ 播放第一个频道 ──→ 全屏显示
    │
    └──→ 否 ──→ 显示"无频道"提示
                        ↓
            用户按【菜单】→ 进入扫描配置
                        ↓
            配置完成 → 开始扫描
                        ↓
            SSE接收频道 ──→ 实时添加到列表
                        ↓
            扫描完成 ──→ 播放第一个频道
```

### 7.2 扫描流程

```
用户配置扫描参数
    ↓
POST /api/scan/execute
    ↓
建立SSE连接 /api/scan/stream
    ↓
接收事件:
    - scan_start: 扫描开始
    - scan_progress: 更新进度条
    - channel_found: 添加频道到列表
    - scan_complete: 扫描完成
    ↓
保存播放列表到本地
```

## 8. 性能目标

| 指标 | 目标值 | 测试方法 |
|------|--------|----------|
| APK大小 | < 5MB | 打包后ls -lh |
| 启动时间 | < 2秒 | 冷启动计时 |
| 运行时内存 | 40-70MB | Android Profiler |
| 切换频道延迟 | < 1秒 | 手动计时 |
| 扫描响应 | 实时 | SSE延迟<100ms |
| 播放缓冲时间 | < 3秒 | 从点击到播放 |

## 9. 兼容性

### 9.1 目标设备

| 设备 | 系统要求 | 测试状态 |
|------|---------|---------|
| 小米电视 E40A | Android 6.0, 1GB RAM | ⭐ 主要目标 |
| 小米盒子3增强版 | Android 5.1 | 待测试 |
| Android Emulator API 19 | Android 4.4 | 开发测试 |

### 9.2 分辨率适配

- **设计基准**: 1920x1080 (1080p)
- **自动缩放**: 支持720p/4K (dp单位)
- **最低支持**: 1280x720

## 10. 文件结构

```
app/src/main/
├── java/com/iptvpro/tv/
│   ├── MainActivity.kt
│   ├── SafeApplication.kt
│   ├── data/
│   │   ├── model/
│   │   │   ├── Channel.kt
│   │   │   ├── ScanConfig.kt
│   │   │   └── ScanProgress.kt
│   │   ├── api/
│   │   │   ├── ApiClient.kt
│   │   │   └── SseClient.kt
│   │   └── cache/
│   │       └── PlayListCache.kt
│   ├── player/
│   │   └── SafeMediaPlayer.kt
│   ├── tv/
│   │   ├── ChannelListFragment.kt
│   │   └── ScanFragment.kt
│   ├── scan/
│   │   └── ScanManager.kt
│   ├── safety/
│   │   ├── CrashHandler.kt
│   │   ├── MemoryMonitor.kt
│   │   └── SafetyCheck.kt
│   └── utils/
│       └── JsonParser.kt
├── res/
│   ├── layout/
│   │   ├── activity_main.xml
│   │   ├── fragment_channels.xml
│   │   └── fragment_scan.xml
│   ├── values/
│   │   ├── colors.xml
│   │   ├── dimens.xml
│   │   └── strings.xml
│   └── drawable/
│       └── *.xml (矢量图标)
└── AndroidManifest.xml
```
