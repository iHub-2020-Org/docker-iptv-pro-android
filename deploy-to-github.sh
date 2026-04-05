#!/bin/bash
# GitHub 仓库创建和代码推送脚本

set -e

echo "=== IPTV Pro Android - GitHub 部署 ==="
echo ""

# 检查必要参数
if [ -z "$GITHUB_TOKEN" ]; then
    echo "❌ 请设置 GITHUB_TOKEN 环境变量"
    echo "   export GITHUB_TOKEN=your_github_token"
    echo ""
    echo "获取 Token: https://github.com/settings/tokens"
    echo "需要权限: repo, workflow"
    exit 1
fi

# 项目信息
PROJECT_NAME="iptv-pro-android"
GITHUB_API="https://api.github.com"

echo "📦 项目名: $PROJECT_NAME"
echo ""

# 1. 创建仓库
echo "创建 GitHub 仓库..."
REPO_RESPONSE=$(curl -s -X POST \
    -H "Authorization: token $GITHUB_TOKEN" \
    -H "Accept: application/vnd.github.v3+json" \
    -d "{\"name\":\"$PROJECT_NAME\",\"private\":false,\"auto_init\":false}" \
    "$GITHUB_API/user/repos")

REPO_URL=$(echo "$REPO_RESPONSE" | grep -o '"html_url":"[^"]*' | head -1 | sed 's/"html_url":"//')
CLONE_URL=$(echo "$REPO_RESPONSE" | grep -o '"clone_url":"[^"]*' | head -1 | sed 's/"clone_url":"//')

echo "✅ 仓库创建成功: $REPO_URL"
echo ""

# 2. 初始化本地仓库
cd /home/reyan/Projects/iptv-pro-android/

echo "📝 初始化 Git 仓库..."
git init 2>/dev/null || true
git remote remove origin 2>/dev/null || true

# 添加远程仓库（包含token）
REMOTE_URL="https://${GITHUB_TOKEN}@github.com/$(echo "$CLONE_URL" | sed 's|https://github.com/||')"
git remote add origin "$REMOTE_URL"

echo "push代码到main分支..."
git branch -M main 2>/dev/null || true
git add -A
git commit -m "Initial commit: IPTV Pro Android v1.0.0" || true
git push -u origin main --force

echo ""
echo "✅ 代码推送成功！"
echo ""

# 3. 获取用户信息
USER_RESPONSE=$(curl -s -H "Authorization: token $GITHUB_TOKEN" \
    -H "Accept: application/vnd.github.v3+json" \
    "$GITHUB_API/user")
USERNAME=$(echo "$USER_RESPONSE" | grep -o '"login":"[^"]*' | sed 's/"login":"//')

echo "🎉 完成！"
echo ""
echo "📎 仓库地址: https://github.com/$USERNAME/$PROJECT_NAME"
echo "⚙️  Actions页面: https://github.com/$USERNAME/$PROJECT_NAME/actions"
echo ""
echo "构建状态："
curl -s -H "Authorization: token $GITHUB_TOKEN" \
    -H "Accept: application/vnd.github.v3+json" \
    "$GITHUB_API/repos/$USERNAME/$PROJECT_NAME/actions/runs" | \
    grep -o '"message":"[^"]*' | head -1 | sed 's/"message":"//' || echo "正在启动..."
echo ""
echo "等待约3-5分钟后，APK将可用"
