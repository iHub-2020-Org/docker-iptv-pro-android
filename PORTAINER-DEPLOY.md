# Portainer Stack 部署指南

## 使用 Portainer Web UI 部署

### 方法1：Portainer Web UI（推荐）

```
1. 登录 Portainer
   https://192.168.1.160:9443
   
2. 进入 Stacks
   左侧菜单 → Stacks → Add Stack
   
3. 配置 Stack
   Name: iptv-pro-builder
   Build method: Web编辑器
   
4. 粘贴内容
   复制 docker-compose.builder.yml 的内容粘贴
   
5. 点击 Deploy the Stack
   
6. 等待构建完成
   Container Logs 中查看构建进度
   
7. 获取APK
   构建完成后 APK 在 /home/reyan/Projects/iptv-pro-android/output/iptv-pro-android.apk
```

### 方法2：使用 Portainer API（命令行）

```bash
# 设置环境变量
export PORTAINER_URL="https://192.168.1.160:9443"
export PORTAINER_TOKEN="ptr_jWE8i4vRaPIB7W9C4nED0grJYX0LZHmbpM7mh/StuNQ="

# 读取 docker-compose 内容
STACK_CONTENT=$(cat docker-compose.builder.yml | jq -s -R .)

# 部署 Stack
curl -X POST "$PORTAINER_URL/api/stacks/create/standalone/string?endpointId=1" \
  -H "Authorization: Bearer $PORTAINER_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"Name\": \"iptv-pro-builder\",
    \"StackFileContent\": $STACK_CONTENT,
    \"Env\": []
  }"
```

### 方法3：本地 Docker Compose

```bash
cd /home/reyan/Projects/iptv-pro-android

# 创建输出目录
mkdir -p output

# 构建并运行
docker-compose -f docker-compose.builder.yml up --build

# APK 输出位置
ls -lh output/iptv-pro-android.apk
```

---

## 部署步骤详解

### Step 1: 准备文件

确保以下文件存在：
- `docker-compose.builder.yml`
- `Dockerfile`
- `app/` 目录（包含完整Android项目）

### Step 2: Portainer 部署

1. 打开浏览器访问 `https://192.168.1.160:9443`
2. 使用凭证登录
3. 选择你的 Docker 环境
4. 点击 Stacks → Add Stack
5. 输入 Name: `iptv-pro-builder`
6. 将 `docker-compose.builder.yml` 内容粘贴到编辑器
7. 点击 Deploy the Stack

### Step 3: 查看构建日志

1. 进入 Containers 页面
2. 找到 `iptv-pro-builder` 容器
3. 点击 Logs 查看构建进度

### Step 4: 获取 APK

```bash
# SSH到Portainer所在服务器
ssh root@192.168.1.160

# 找到APK
docker cp iptv-pro-builder:/output/iptv-pro-android.apk ~/iptv-pro-android.apk

# 或者找到挂载的卷位置docker inspect iptv-pro-builder | grep Mounts
# APK 将在挂载的卷中
```

APIK 实际位置在宿主机：**`/home/reyan/Projects/iptv-pro-android/output/iptv-pro-android.apk`**

### Step 5: 安装到电视

```bash
adb connect <TV-IP>
adb install iptv-pro-android.apk
```

---

## 故障排查

| 问题 | 解决 |
|------|------|
| 构建失败 | 查看容器日志，可能是网络问题 |
| 权限错误 | 确保卷挂载权限正确 |
| 内存不足 | 增加Docker内存限制 |
| APK未生成 | 检查 /output 目录挂载 |

---

## 清理

```bash
# 停止并删除 Stack
docker-compose -f docker-compose.builder.yml down

# 或者在Portainer中删除
docker rm -f iptv-pro-builder
```
