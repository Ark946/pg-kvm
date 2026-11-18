# preserve-resolution-selection 测试结果

- 执行日期：2026-11-18
- 环境：Windows 11、JDK 17、Gradle 8.9 / AGP 8.3.2
- 测试类型：Kotlin/JUnit 单元测试 + 编译/构建 + 代码审查；真机用例待 UVC 硬件验证

## 1. 自动化单元测试

命令：

```powershell
cd pg-kvm-android
.\gradlew.bat :app:testDebugUnitTest --tests "*CaptureSpecTest*" --console=plain
```

结果：**5 通过 / 0 失败**（`app/build/test-results/testDebugUnitTest/TEST-...CaptureSpecTest.xml`）。

| 用例 | 结果 | 说明 |
| --- | --- | --- |
| U-01 首选规格与支持列表宽高匹配 | ✅ 通过 | 返回 1920x1080 项 |
| U-02 尺寸匹配但最大帧率不同 | ✅ 通过 | 返回该尺寸；应用时保留用户选择的帧率 |
| U-03 支持列表为空 | ✅ 通过 | 返回 `null` |
| U-04 首选尺寸不在列表中 | ✅ 通过 | 返回 `null` |
| U-05 默认规格常量 | ✅ 通过 | `CaptureSpec.DEFAULT = 1280x720@30` |

### 测试驱动的行为修正

初始实现按“宽高+帧率完全匹配”。测试发现支持列表对每个尺寸只暴露最大帧率，
同一分辨率在不同摄像头上可能报告不同最大帧率，导致首选规格被误判为不支持。
因此改为按宽高匹配、应用时保留首选帧率；U-02 先失败后通过。

## 2. 编译与静态检查

| 用例 | 结果 | 证据 |
| --- | --- | --- |
| BT-01 Kotlin 编译 | ✅ 通过 | `.\gradlew.bat :app:compileDebugKotlin` |
| BT-02 完整构建 | ✅ 通过 | `.\gradlew.bat :app:assembleDebug` |
| BT-03 启动路径使用保存规格 | ✅ 通过 | `startCapturer` / `resumeCapture` / `changeResolution` 均读写 `captureSpec` |
| BT-04 状态回报 | ✅ 通过 | 启动、恢复、切换分辨率均调用 `onCaptureStarted` |
| BT-05 首选更新时机 | ✅ 通过 | `switchResolution` 仅在 `changeResolution` 返回 true 时更新首选 |
| BT-06 激活时回退策略 | ✅ 通过 | `activateCamera` 先 `resetCaptureSpec()`；支持列表就绪后应用首选，不支持保持默认 |

补充：`UvcCameraVideoCapture.uvcCamera` 标记为 `volatile`，保证管理器轮询支持列表时能看到采集线程的写入。

## 3. 真机端到端

模拟器没有 UVC 摄像头，以下用例必须在连接采集卡的真机执行：

| 用例 | 状态 | 期望 |
| --- | --- | --- |
| MAN-01 选择 1920x1080@30 → 关闭/开启预览 | ⏳ 待验证 | 仍为 1920x1080@30 |
| MAN-02 选择 1920x1080@30 → 停止/重新开始拉流 | ⏳ 待验证 | 仍为 1920x1080@30 |
| MAN-03 摄像头 A 选 1920x1080 → 切到支持该分辨率的 B | ⏳ 待验证 | B 以 1920x1080 采集 |
| MAN-04 切到不支持该分辨率的 C | ⏳ 待验证 | C 回退默认；首选不被覆盖 |
| MAN-05 每次切换后状态 | ⏳ 待验证 | 分辨率/帧率与实际采集一致 |

## 4. 结论

- 实现、单元测试、编译与完整构建均通过。
- 真机用例 MAN-01~05 待验证；通过后归档本变更并创建 PR。
