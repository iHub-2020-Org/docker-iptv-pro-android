# GitHub 仓库设置指南

## 第1步：创建 GitHub 仓库

```bash
# 1. 在 GitHub 网页上创建新仓库
# 访问: https://github.com/new
# 仓库名: iptv-pro-android
# 选择: Public 或 Private
# 不要初始化 README（我们已有）
```

## 第2步：推送本地代码

```bash
cd /home/reyan/Projects/iptv-pro-android

# 初始化 Git 仓库
git init

# 添加所有文件
git add .

# 提交
git commit -m "Initial commit: IPTV Pro Android v1.0.0"

# 添加远程仓库（替换 YOUR_USERNAME）
git remote add origin https://github.com/YOUR_USERNAME/iptv-pro-android.git

# 推送代码
git branch -M main
git push -u origin main
```

## 第3步：触发自动构建

推送完成后，GitHub Actions 会自动开始构建 APK。

```
1. 访问: https://github.com/YOUR_USERNAME/iptv-pro-android/actions
2. 等待构建完成（约3-5分钟）
3. 点击最新的 workflow run
4. 在 Artifacts 中下载 APK
```

## 第4步：下载 APK

构建成功后：
- 点击 workflow run
- 找到 "iptv-pro-android" artifact
- 点击下载 ZIP 文件
- 解压后得到 `app-release.apk`

## 完整命令行示例

```bash
# 进入项目目录
cd /home/reyan/Projects/iptv-pro-android

# 初始化并推送
git init
git add .
git commit -m "Initial commit"
git remote add origin https://github.com/YOUR_USERNAME/iptv-pro-android.git
git branch -M main
git push -u origin main

# 等待 GitHub Actions 构建
# 访问 Actions 页面下载 APK
```

## 注意事项

1. **替换 YOUR_USERNAME** 为你的 GitHub 用户名
2. **GitHub Actions 免费额度**：Public 仓库无限免费
3. 首次推送可能需要登录验证
