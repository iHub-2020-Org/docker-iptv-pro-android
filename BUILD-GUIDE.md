# IPTV Pro Android - 构建指南

## 方式1：Docker 构建（推荐）

```bash
cd /home/reyan/Projects/iptv-pro-android

# 构建 Docker 镜像
docker build -t iptv-builder .

# 运行构建
docker run --rm -v $(pwd)/app:/app iptv-builder

# 输出 APK
# ./app/iptv-pro-release.apk
```

## 方式2：本地构建

### 前提条件
- JDK 11+
- Android SDK（API 19-28）
- Gradle

```bash
cd /home/reyan/Projects/iptv-pro-android/app

# 构建
./gradlew assembleRelease

# 输出
# app/build/outputs/apk/release/app-release.apk
```

## 方式3：GitHub Actions 自动构建

```bash
# 推送到 GitHub 后自动构建
git init
git add .
git commit -m "Initial commit"
git push origin main

# 在 GitHub Actions 页面下载 APK
```

## APK 输出

构建成功后：
- **路径**: `iptv-pro-release.apk`
- **目标大小**: < 5MB
- **安装**: `adb install iptv-pro-release.apk`
