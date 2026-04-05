# IPTV Pro Android - 验证计划

## 验证级别

### Level 1: 单元测试

| 测试项 | 测试方法 | 通过标准 |
|--------|----------|----------|
| Channel.toJson/fromJson | JUnit | JSON序列化正确 |
| PlayListCache.save/load | JUnit + Mock Context | 数据持久化正常 |
| Config.Endpoints | 编译检查 | 所有端点定义正确 |

### Level 2: 集成测试

| 测试项 | 测试方法 | 通过标准 |
|--------|----------|----------|
| ApiClient.getSync | 模拟服务器 | 返回200 |
| ApiClient.postAsync | 模拟服务器 | 回调正常 |
| SseClient.connect | 模拟SSE流 | 事件解析正确 |
| PlayListCache + ApiClient | 组合测试 | 完整流程 |

### Level 3: 系统测试

| 测试项 | 测试方法 | 通过标准 |
|--------|----------|----------|
| 启动流程 | 冷启动 | 2秒内显示界面 |
| 模式切换 | 遥控器操作 | PLAY/LIST/SCAN切换正常 |
| 频道播放 | 真实频道 | 视频正常播放 |
| 扫描流程 | 完整扫描 | SSE接收频道并保存 |
| 异常恢复 | 注入异常 | 安全模式激活 |

### Level 4: 性能测试

| 指标 | 目标 | 测试工具 |
|------|------|----------|
| APK大小 | < 5MB | ls -lh |
| 启动时间 | < 2秒 | 手动计时 |
| 运行时内存 | 40-70MB | Android Profiler |
| 切换频道 | < 1秒 | 手动计时 |
| 播放缓冲 | < 3秒 | 手动计时 |

### Level 5: 安全测试

| 测试项 | 测试方法 | 预期结果 |
|--------|----------|----------|
| 内存不足 | 模拟低内存 | 进入安全模式 |
| 崩溃恢复 | 注入RuntimeException | 安全模式提示 |
| 网络超时 | 断开网络 | 显示错误提示 |
| 播放器错误 | 播放无效URL | 错误回调触发 |
| ANR防护 | 阻塞主线程 | 无ANR |

### Level 6: 设备测试

| 设备 | 测试项 | 结果 |
|------|--------|------|
| Android Emulator API 19 | 全部功能 | ⬜ |
| Android Emulator API 28 | 全部功能 | ⬜ |
| 小米电视 E40A | 全部功能 | ⬜ |

## 测试脚本

```bash
#!/bin/bash
# 验证APK大小
APK_SIZE=$(stat -c%s "app/build/outputs/apk/release/app-release.apk")
if [ $APK_SIZE -lt 5242880 ]; then
    echo "✅ APK size OK: $APK_SIZE bytes"
else
    echo "❌ APK too large: $APK_SIZE bytes"
fi
```

## 验收标准

- [ ] 所有单元测试通过
- [ ] 系统测试无崩溃
- [ ] 内存占用<80MB
- [ ] APK大小<5MB
- [ ] 真机测试通过
