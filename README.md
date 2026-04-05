# IPTV Pro Android

Android TV 客户端，配合 [iptv-scanner-pro](https://github.com/iHub-2020-Org/iptv-scanner-pro) 后端使用。

## 快速开始

### 1. 设置后端 URL（GitHub Secret）

```
Settings → Secrets and variables → Actions → New secret
Name:  IPTV_BASE_URL
Value: http://你的服务器IP:5950
```

### 2. 构建 APK

在 GitHub → Actions → Build APK → Run workflow，选择 `release`。  
构建完成后在 Artifacts 下载 `iptv-pro-release.zip`，解压得到 APK。

### 3. 安装到电视

```bash
adb connect 电视IP:5555
adb install iptv-pro-release.apk
```

## 遥控器操作

| 按键 | 播放模式 | 频道列表 | 扫描模式 |
|------|---------|---------|---------|
| ← → | 上/下一台 | — | — |
| ↑ ↓ | 音量 | — | — |
| OK | 打开频道列表 | 播放选中 | 开始/停止扫描 |
| MENU | 打开扫描 | 打开扫描 | — |
| INFO | 切换信息栏 | — | — |
| BACK/← | — | 返回播放 | 返回播放 |

## 架构说明

详见 [docs/architecture.md](docs/architecture.md)

## 依赖

- **后端**：iptv-scanner-pro（必须，负责 IPv6 CDN 代理）
- **第三方库**：无（仅 Android 系统 API）
- **最低 Android 版本**：5.0 (API 21)
