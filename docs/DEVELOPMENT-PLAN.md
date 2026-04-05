# IPTV Pro Android - 开发计划

## 项目信息

| 属性 | 值 |
|------|----|
| **项目名称** | IPTV Pro Android |
| **目标设备** | 小米电视 E40A (1GB RAM) |
| **APK目标** | < 5MB |
| **内存目标** | 40-70MB |
| **开发周期** | 14天 |
| **后端地址** | http://192.168.9.158:5950 |

---

## 开发阶段

### Phase 1: 项目搭建 (Day 1-2)

#### Day 1: 基础架构

| 任务 | 产出 | 验证 |
|------|------|------|
| 创建项目结构 | 完整目录树 | ls检查 |
| 配置build.gradle | 编译配置 | 同步成功 |
| 创建Application类 | SafeApplication.kt | 编译通过 |
| 配置AndroidManifest | 权限声明 | lint通过 |

```groovy
// build.gradle 配置要点
defaultConfig {
    applicationId "com.iptvpro.tv"
    minSdk 19
    targetSdk 28
}
buildTypes.release {
    minifyEnabled true
    shrinkResources true
}
// 零第三方依赖
dependencies {
    // 空 - 纯系统API
}
```

#### Day 2: 基础工具

| 任务 | 产出 | 验证 |
|------|------|------|
| 创建数据模型 | Channel.kt, ScanConfig.kt | 单元测试 |
| 创建Constants配置 | Config.kt | 编译检查 |
| JSON解析工具 | JsonParser.kt | 解析测试 |
| 日志工具 | Logger.kt | 输出检查 |

---

### Phase 2: 数据层 (Day 3-4)

#### Day 3: HTTP客户端

| 任务 | 产出 | 验证 |
|------|------|------|
| 实现ApiClient | ApiClient.kt | mock测试 |
| GET请求封装 | getSync() | 响应检查 |
| POST请求封装 | postAsync() | 回调测试 |
| 服务器检测 | isServerAvailable() | 连通测试 |

**关键验证:**
```kotlin
// 验证点1: GET请求
val response = ApiClient.getSync("/api/health")
assert(response != null)
assert(response.contains("healthy"))

// 验证点2: POST请求
ApiClient.postAsync("/api/scan/execute", json) { result ->
    assert(result != null)
}
```

#### Day 4: SSE客户端

| 任务 | 产出 | 验证 |
|------|------|------|
| 实现SseClient | SseClient.kt | 连接测试 |
| 事件解析 | event/data解析 | 日志验证 |
| 连接管理 | connect/disconnect | 状态检查 |
| 线程安全 | Handler主线程回调 | ANR检查 |

**关键验证:**
```kotlin
// 验证SSE连接
val sse = SseClient { event, data ->
    Log.d("SSE", "Received: $event")
}
sse.connect("/api/scan/stream")
// 等待事件... 5秒后断开
sse.disconnect()
```

---

### Phase 3: 安全层 (Day 5)

#### Day 5: 安全防护

| 任务 | 产出 | 验证 |
|------|------|------|
| SafetyCheck | 启动前检查 | 模拟低内存测试 |
| CrashHandler | 全局异常捕获 | Exception注入测试 |
| MemoryMonitor | 运行时监控 | Profiler观察 |
| SafeModeManager | 安全模式管理 | SP读写测试 |
| BackupManager | 数据备份 | 文件操作验证 |

**关键验证:**
```kotlin
// 内存检查
val pass = SafetyCheck.checkBeforeLaunch(context)
assert(pass == true) // 正常环境应该通过

// 异常捕获
throw RuntimeException("Test")
// 检查是否触发Handler，应用是否优雅退出
```

---

### Phase 4: 播放器 (Day 6-7)

#### Day 6: MediaPlayer封装

| 任务 | 产出 | 验证 |
|------|------|------|
| 实现SafeMediaPlayer | SafeMediaPlayer.kt | 播放测试 |
| Surface绑定 | setSurface() | 显示检查 |
| 错误监听 | setOnErrorListener | 错误注入 |
| 超时处理 | prepare超时5秒 | 超时测试 |
| 资源释放 | stop()内存清理 | Profiler验证 |

**关键验证:**
```kotlin
// 播放测试
val player = SafeMediaPlayer(surfaceView) { 
    // onError
}
player.play(testChannel)
Thread.sleep(10000) // 播放10秒
player.stop()
// 检查内存是否回落
```

#### Day 7: 播放器UI

| 任务 | 产出 | 验证 |
|------|------|------|
| SurfaceView布局 | activity_main.xml | 全屏显示 |
| 底部信息栏 | 频道名称/分辨率 | 遥控器焦点 |
| 音量控制 | AudioManager | 按键测试 |
| 频道切换 | 左右键处理 | 切换测试 |

---

### Phase 5: 频道列表 (Day 8-9)

#### Day 8: 列表组件

| 任务 | 产出 | 验证 |
|------|------|------|
| 网格布局 | fragment_channels.xml | GridView显示 |
| ChannelAdapter | 频道列表Adapter | 数据绑定 |
| 焦点处理 | 选中态高亮 | 遥控器测试 |
| 加载缓存 | PlayListCache | 数据持久化 |

**关键验证:**
```kotlin
// 列表加载
val channels = PlayListCache.load(context)
assert(channels.size > 0)
// GridView显示
// 遥控器上下左右移动焦点
```

#### Day 9: 列表整合

| 任务 | 产出 | 验证 |
|------|------|------|
| Fragment嵌入 | ChannelListFragment | 切换测试 |
| MODE_LIST切换 | switchMode() | 状态检查 |
| 播放回调 | onChannelClick | 播放验证 |
| 返回处理 | KEYCODE_BACK | 回退测试 |

---

### Phase 6: 扫描功能 (Day 10-11)

#### Day 10: 扫描界面

| 任务 | 产出 | 验证 |
|------|------|------|
| 扫描配置UI | fragment_scan.xml | 表单显示 |
| 模板选择 | Spinner组件 | 选择测试 |
| 范围输入 | EditText | 输入验证 |
| 参数滑块 | SeekBar | 数值调整 |

#### Day 11: 扫描逻辑

| 任务 | 产出 | 验证 |
|------|------|------|
| ScanManager | 扫描管理器 | 流程测试 |
| 启动扫描 | postAsync调用 | 后端响应 |
| 进度接收 | SSE progress事件 | 进度更新 |
| 结果保存 | PlayListCache.save | 数据验证 |
| 自动切换 | 扫描完成切播放 | 流程验证 |

**关键验证:**
```kotlin
// 启动扫描
ScanManager.start(config) { progress ->
    Log.d("SCAN", "Progress: ${progress.checked}/${progress.total}")
}
// 等待实时频道...
// 验证频道被添加到列表
```

---

### Phase 7: 主控制器 (Day 12-13)

#### Day 12: MainActivity整合

| 任务 | 产出 | 验证 |
|------|------|------|
| Activity框架 | MainActivity.kt | 编译通过 |
| 模式切换 | switchMode() | 布局切换 |
| 按键映射 | onKeyDown() | 全键位测试 |
| 启动流程 | onCreate() | 启动测试 |
| 安全模式入口 | showSafeModeDialog() | 触发测试 |

#### Day 13: 整合测试

| 任务 | 产出 | 验证 |
|------|------|------|
| 完整流程 | 端到端集成 | 手测 |
| 内存检测 | Profiler监控 | <80MB |
| 崩溃注入 | 异常模拟 | 安全模式验证 |
| 边界测试 | 无网络/无频道 | 容错验证 |

---

### Phase 8: 打包发布 (Day 14)

#### Day 14: 最终交付

| 任务 | 产出 | 验证 |
|------|------|------|
| 资源优化 | 压缩图片/XML | APK<5MB |
| ProGuard配置 | proguard-rules.pro | 混淆正确 |
| 签名打包 | release.apk | 签名验证 |
| 文档生成 | 全部文档完成 | md检查 |
| 验证报告 | QA-CHECKLIST | 条目核对 |

---

## 代码审查清单

### 每个Task完成前必须检查

- [ ] 编译无警告/错误
- [ ] 代码符合规范
- [ ] 单元测试通过
- [ ] 内存无泄漏
- [ ] 异常处理完善
- [ ] 文档已更新

---

## 风险预案

| 风险 | 应对 |
|------|------|
| MediaPlayer不稳定 | 替换为ExoPlayer Core (备选方案) |
| SSE连接不稳定 | 增加自动重连机制 |
| 后端服务不可达 | 增加离线模式支持 |
| 遥控器按键冲突 | 调整按键映射 |
| 内存超标 | 启用安全模式限制 |

---

## 交付物清单

### 代码
- [ ] app/ 完整Android项目
- [ ] 零第三方依赖
- [ ] Kotlin源码文件
- [ ] XML布局文件

### 文档
- [ ] README.md
- [ ] DESIGN.md
- [ ] SECURITY.md
- [ ] DEVELOPMENT-PLAN.md
- [ ] VALIDATION-PLAN.md
- [ ] QA-CHECKLIST.md
- [ ] EXECUTION-LOG.md

### 构建
- [ ] release.apk (<5MB)
- [ ] proguard-rules.pro
- [ ] 签名密钥

### 测试
- [ ] 单元测试报告
- [ ] 集成测试报告
- [ ] 真机测试报告
