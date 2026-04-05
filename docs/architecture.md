# IPTV Pro Android — 架构文档

## 项目概述

Android TV 客户端，连接 iptv-scanner-pro 后端，实现频道列表浏览与播放。

## 整体架构

```
┌────────────────────────────────────────────────────────┐
│  Android TV App (com.iptvpro.tv)                       │
│                                                        │
│  MainActivity                                          │
│  ├─ MODE_PLAY  → SurfaceView + SafeMediaPlayer         │
│  ├─ MODE_LIST  → ListView + ArrayAdapter<String>       │
│  └─ MODE_SCAN  → SSE + SseClient → Scanner API        │
│                                                        │
│  Data Layer                                            │
│  ├─ Config.kt        (BASE_URL via BuildConfig)        │
│  ├─ ApiClient.kt     (HTTP GET/POST, no 3rd-party)     │
│  ├─ SseClient.kt     (SSE event stream)                │
│  └─ PlayListCache.kt (SharedPreferences persistence)   │
│                                                        │
│  Player Layer                                          │
│  └─ SafeMediaPlayer  → Android MediaPlayer             │
│       ★ Uses PROXY URL (not raw CDN URL)              │
└────────────────────────────────────────────────────────┘
               ↕ HTTP (LAN: 192.168.x.x:5950)
┌────────────────────────────────────────────────────────┐
│  iptv-scanner-pro (Go backend on pve-vm-media)         │
│  ├─ /api/results         → 返回已扫描频道列表           │
│  ├─ /api/scan/execute    → 启动扫描                    │
│  ├─ /api/scan/stream     → SSE 实时推送                │
│  ├─ /api/scan/stop       → 停止扫描                    │
│  └─ /api/proxy/stream    → M3U8 代理 (★ 关键)         │
│       ★ 处理 IPv6 CDN 重定向                           │
│       ★ 重写 TS 片段 URL 为代理 URL                    │
└────────────────────────────────────────────────────────┘
               ↕ IPv6 (ott.mobaibox.com CDN)
┌────────────────────────────────────────────────────────┐
│  中国移动 IPTV CDN (IPv6)                               │
│  ott.mobaibox.com → 302 → [2409:...]:80/...            │
└────────────────────────────────────────────────────────┘
```

## 关键设计决策

### 1. 播放 URL 必须走代理 ★★★

**错误方式（原始 bug）**：直接使用原始 IPTV URL
```
channel.url = "http://ott.mobaibox.com/PLTV/4/224/322122XXXX/index.m3u8"
```
问题：中国移动 IPTV 走 IPv6 CDN，电视机通常只有 IPv4，直连必然失败。

**正确方式（已修复）**：通过后端代理
```kotlin
// SafeMediaPlayer.buildProxyUrl()
val encoded = Uri.encode(channel.url)
val proxyUrl = "${Config.BASE_URL}${Config.Endpoints.PROXY_STREAM}?url=$encoded"
// 实际 URL: http://192.168.9.158:5950/api/proxy/stream?url=http%3A%2F%2Fott...
```

后端代理负责：
- 跟随 302 重定向到 IPv6 CDN
- 重写 M3U8 内 TS 片段 URL 为 `/api/proxy/segment?url=...`
- 代理 TS 片段下载（强制 `video/MP2T` MIME 类型）

Android MediaPlayer 的请求链：
```
TV → GET /api/proxy/stream?url=... → Server → IPv6 CDN (M3U8)
TV → GET /api/proxy/segment?url=... → Server → IPv6 CDN (TS)
```

### 2. 零第三方依赖

不引入 ExoPlayer / OkHttp / Retrofit。原因：
- 电视机内存有限（256MB~512MB RAM）
- APK 大小目标 <5MB
- Android `MediaPlayer` 配合代理 URL 已足够可靠
- 减少 GC 压力

### 3. 线程模型

```
Main Thread (UI)
├─ 所有 View 更新
├─ Player 回调 (via mainHandler.post)
└─ SafetyCheck / PlayListCache

Background Threads
├─ Thread { fetchFromServer() }    ← 启动时拉取服务器频道
├─ SafeMediaPlayer 内部线程         ← MediaPlayer 异步准备
├─ SseClient.thread                ← SSE 事件监听
│    └─ mainHandler.post { channels.add() } ← 必须回主线程
└─ ApiClient.postAsync(Thread{})   ← HTTP POST 请求
```

**注意**：`channels.add()` 必须在主线程执行，SseClient 回调通过 `mainHandler.post` 保证。

### 4. UI 模式

| 模式 | 触发键 | 显示内容 |
|------|--------|---------|
| MODE_PLAY (0) | 初始/返回 | SurfaceView 全屏 + 底部 InfoBar |
| MODE_LIST (1) | OK/确定 | ListView 频道列表 |
| MODE_SCAN (2) | MENU | 扫描控制 + 实时进度 |

### 5. 频道加载策略

```
onCreate()
 ├─ PlayListCache.load()       → 快速本地缓存（SharedPreferences）
 └─ Thread { fetchFromServer } → 后台刷新（/api/results）
      └─ 若服务器频道数更多 → 更新缓存 + 刷新 UI
```

## 已知限制

| 限制 | 说明 |
|------|------|
| 频道名称 | 依赖后端 channels.go 匹配库，未匹配显示"频道 XXXX" |
| 扫描模板 | 硬编码第一个模板 + 范围 0000-0100，需 UI 支持选择 |
| HLS 支持 | 依赖 Android MediaPlayer，部分 TV 固件 HLS 实现不完整 |
| 网络依赖 | 必须与后端同一局域网；后端需具备 IPv6 |

## 遥控器按键映射

```
播放模式:
  ← →       切换上/下一台
  ↑ ↓       音量 +/-
  OK        打开频道列表
  MENU      打开扫描界面
  INFO      切换信息栏显示

频道列表:
  ↑ ↓       上下移动
  OK        播放选中频道
  ←/BACK    返回播放

扫描模式:
  OK        开始/停止扫描
  ←/BACK    返回播放
```
