# honor-configured-ports 测试资产

本目录保存 `honor-configured-ports` 变更的测试计划、用例、夹具与结果。

## 目录结构

```text
test/honor-configured-ports/
├── plan.md                  # 需求澄清、方案摘要、测试用例
├── results.md               # 实际执行结果与发现的问题
├── automated/
│   ├── ports.test.ts        # 端口解析 / WS URL 拼接单元测试
│   └── device-status.test.ts# /api/status 响应结构校验单元测试
├── fixtures/
│   ├── stream_server_prefs.xml          # 信令 4000 / 输入 4001 的设备偏好（模拟器用）
│   └── stream_server_prefs.default.xml  # 默认 3000 / 3001
└── README.md
```

## 运行自动化测试

```powershell
# 在仓库根目录执行
node --test test\honor-configured-ports\automated\ports.test.ts test\honor-configured-ports\automated\device-status.test.ts
```

## 模拟器夹具说明

`fixtures/*.xml` 对应应用 `shared_prefs/stream_server_prefs.xml` 的内容，用于在模拟器中把
信令/输入端口切换为 4000/4001 或恢复默认值：

```powershell
adb push test\honor-configured-ports\fixtures\stream_server_prefs.xml /data/local/tmp/stream_server_prefs.xml
adb shell run-as io.getstream.webrtc.sample.compose cp /data/local/tmp/stream_server_prefs.xml shared_prefs/stream_server_prefs.xml
```

写入后重启应用生效。真机验证应优先使用应用内「服务器设置」对话框修改端口。
