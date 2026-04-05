# IPTV Pro Android - 安全规范

## 概述

本应用专为**低配置电视设备**（如小米E40A，1GB内存）设计，安全性是首要考量。任何崩溃或资源占用过高都可能导致电视系统不稳定。

## 威胁模型

| 风险类型 | 严重性 | 防护策略 |
|----------|--------|----------|
| **内存耗尽** | 高 | 启动检查 + 运行时监控 |
| **ANR无响应** | 高 | 异步任务 + 超时处理 |
| **播放器崩溃** | 中 | try-catch + 自动恢复 |
| **系统崩溃** | 高 | 全局异常捕获 + 安全模式 |
| **数据损坏** | 低 | 备份机制 + 容错解析 |

## 防护机制

### Level 1: 启动前检查

```kotlin
object SafetyCheck {
    fun checkBeforeLaunch(context: Context): Boolean {
        // 1. 内存检查
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory() / 1024 / 1024
        val freeMemory = runtime.freeMemory() / 1024 / 1024
        
        if (freeMemory < 50) {
            // 内存不足50MB，进入安全模式
            return false
        }
        
        // 2. 检查上次是否崩溃
        val prefs = context.getSharedPreferences("safe", Context.MODE_PRIVATE)
        val lastCrashed = prefs.getBoolean("last_crashed", false)
        
        if (lastCrashed) {
            // 上次崩溃，进入安全模式
            prefs.edit().putBoolean("last_crashed", false).apply()
            return false
        }
        
        // 3. 检查Android版本
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            // Android 4.4以下不支持
            return false
        }
        
        return true
    }
}
```

### Level 2: 全局异常捕获

```kotlin
class SafeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // 记录崩溃信息
            Log.e("CRASH", "Uncaught exception in thread: ${thread.name}", throwable)
            
            // 设置崩溃标记
            getSharedPreferences("safe", MODE_PRIVATE)
                .edit()
                .putBoolean("last_crashed", true)
                .putString("crash_time", SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date()))
                .apply()
            
            // 优雅退出
            Process.killProcess(Process.myPid())
            exitProcess(1)
        }
    }
}
```

### Level 3: 运行时内存监控

```kotlin
class MemoryMonitor(private val context: Context) {
    private val handler = Handler(Looper.getMainLooper())
    private var isMonitoring = false
    
    private val checkRunnable = object : Runnable {
        override fun run() {
            checkMemory()
            if (isMonitoring) {
                handler.postDelayed(this, 5000) // 每5秒检查
            }
        }
    }
    
    fun start() {
        isMonitoring = true
        handler.post(checkRunnable)
    }
    
    fun stop() {
        isMonitoring = false
        handler.removeCallbacks(checkRunnable)
    }
    
    private fun checkMemory() {
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory() / 1024 / 1024
        val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
        val freeMemory = maxMemory - usedMemory
        
        Log.d("MEMORY", "Used: ${usedMemory}MB, Free: ${freeMemory}MB, Max: ${maxMemory}MB")
        
        // 内存不足警告
        if (freeMemory < 30) {
            // 触发清理
            clearCaches()
            
            // 仍不足则提示
            if (freeMemory < 20) {
                showLowMemoryWarning()
            }
        }
    }
    
    private fun clearCaches() {
        // 清理图片缓存
        // 清理播放历史
        // 提示用户
    }
}
```

### Level 4: 播放器安全封装

```kotlin
class SafeMediaPlayer(...) {
    private val timeoutHandler = Handler(Looper.getMainLooper())
    private var isPreparing = false
    
    private val timeoutRunnable = Runnable {
        if (isPreparing) {
            Log.e("PLAYER", "Prepare timeout")
            stop()
            onError()
        }
    }
    
    fun play(channel: Channel) {
        try {
            stop() // 清理旧实例
            
            mediaPlayer = MediaPlayer().apply {
                // 错误监听
                setOnErrorListener { _, what, extra ->
                    handleError(what, extra)
                    true
                }
                
                // 准备监听
                setOnPreparedListener {
                    isPreparing = false
                    timeoutHandler.removeCallbacks(timeoutRunnable)
                    start()
                }
                
                // 设置超时
                isPreparing = true
                timeoutHandler.postDelayed(timeoutRunnable, 5000)
                
                prepareAsync()
            }
        } catch (e: Exception) {
            handleError(-1, -1)
        }
    }
    
    private fun handleError(what: Int, extra: Int) {
        Log.e("PLAYER", "Error: what=$what, extra=$extra")
        stop()
        onError()
    }
}
```

### Level 5: 安全模式

```kotlin
object SafeModeManager {
    private const val PREFS_SAFE = "safe_mode"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_REASON = "reason"
    
    // 进入安全模式
    fun enterSafeMode(context: Context, reason: String) {
        context.getSharedPreferences(PREFS_SAFE, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, true)
            .putString(KEY_REASON, reason)
            .apply()
    }
    
    // 检查是否在安全模式
    fun isInSafeMode(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_SAFE, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, false)
    }
    
    // 退出安全模式
    fun exitSafeMode(context: Context) {
        context.getSharedPreferences(PREFS_SAFE, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, false)
            .remove(KEY_REASON)
            .apply()
    }
    
    // 安全模式行为
    fun applySafeModeRestrictions(activity: Activity) {
        // 1. 禁用所有动画
        activity.window.setWindowAnimations(0)
        
        // 2. 降低播放质量（如果支持）
        // 3. 禁用频道封面加载
        // 4. 限制频道列表数量
        // 5. 显示安全模式提示
    }
}
```

### Level 6: 数据保护

```kotlin
object BackupManager {
    private const val BACKUP_DIR = "/sdcard/Android/data/com.iptvpro.tv.backup/"
    
    // 自动备份播放列表
    fun backupPlaylists(context: Context) {
        try {
            val backupDir = File(BACKUP_DIR)
            backupDir.mkdirs()
            
            // 复制播放列表文件
            val playlistsDir = File(context.filesDir, "playlists")
            playlistsDir.listFiles()?.forEach { file ->
                file.copyTo(File(backupDir, file.name), overwrite = true)
            }
            
            // 备份SharedPreferences
            val prefs = context.getSharedPreferences("channels", Context.MODE_PRIVATE)
            val backupFile = File(backupDir, "channels_backup.json")
            backupFile.writeText(prefs.getString("playlist_json", "[]")!!)
            
        } catch (e: Exception) {
            Log.e("BACKUP", "Backup failed", e)
        }
    }
    
    // 恢复备份
    fun restorePlaylists(context: Context): Boolean {
        return try {
            val backupFile = File(BACKUP_DIR, "channels_backup.json")
            if (backupFile.exists()) {
                val json = backupFile.readText()
                context.getSharedPreferences("channels", Context.MODE_PRIVATE)
                    .edit()
                    .putString("playlist_json", json)
                    .apply()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}
```

## 应急预案

### 场景1: 播放时ANR

```
症状: 视频卡住，遥控器无响应
解决: 
1. 等待系统自动显示"应用无响应"对话框
2. 选择"等待"（可能恢复）
3. 选择"关闭应用"（重启应用）

防护:
- 播放器准备5秒超时自动释放
- 播放错误自动切换下个频道
```

### 场景2: 内存不足崩溃

```
症状: 应用闪退，回到电视桌面
解决:
1. 重启应用（会自动进入安全模式）
2. 在安全模式下：
   - 最少功能集
   - 无频道封面
   - 简化UI
   
防护:
- 启动内存检查
- 运行时内存监控
- 自动清理缓存
```

### 场景3: 系统卡顿

```
症状: 整个电视变卡，包括其他应用
解决:
1. 长按Home键
2. 强制停止IPTV Pro Android
3. 重启电视

防护:
- 严格内存限制
- 不后台驻留
- 退出时完全释放
```

## 安全级别

| 级别 | 触发条件 | 行为 |
|------|----------|------|
| **正常** | 内存>100MB | 全部功能可用 |
| **警告** | 内存70-100MB | 提示内存不足 |
| **受限** | 内存50-70MB | 禁用封面加载 |
| **安全模式** | 内存<50MB 或上次崩溃 | 仅基本播放功能 |
| **禁止启动** | 内存<30MB 或Android<4.4 | 显示错误并退出 |

## 恢复流程

```
崩溃发生
    ↓
设置 last_crashed = true
    ↓
下次启动检测到标记
    ↓
显示对话框:
"检测到上次异常退出，
已进入安全模式。
[继续安全模式] [恢复正常模式]"
    ↓
用户选择继续或恢复
```

## 注意事项

1. **禁止在后台运行** - 退出即完全释放
2. **禁止自动更新** - 避免后台下载
3. **禁止推送通知** - 减少系统负担
4. **定期清理缓存** - 防止存储耗尽

## 测试要求

- [ ] 连续播放24小时不崩溃
- [ ] 内存占用峰值<80MB
- [ ] 崩溃后安全模式正常进入
- [ ] 32位和64位ARM都兼容
