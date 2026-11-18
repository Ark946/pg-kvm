# preserve-resolution-selection 测试计划

## 1. 需求澄清

| 编号 | 澄清项 | 结论 |
| --- | --- | --- |
| RQ-1 | “保持规格”的范围 | 宽、高、帧率三项；用户从支持列表中选择的完整规格 |
| RQ-2 | 哪些操作后必须保持 | 同一摄像头的暂停/恢复预览、停止/重新开始串流 |
| RQ-3 | 切换摄像头 | 使用全局首选规格；新摄像头支持则应用，否则回退默认 1280x720@30 |
| RQ-4 | 不支持首选时是否覆盖选择 | 不覆盖；只本次回退默认，之后切回支持该规格的摄像头仍应恢复首选 |
| RQ-5 | 跨应用重启 | 不持久化；重启后使用默认规格（Non-Goal） |
| RQ-6 | 状态展示 | `StreamInfo` 的分辨率/帧率必须与最近一次实际启动采集所用规格一致 |
| RQ-7 | 无 capturer 时恢复 | `resumeCapture()` 保持 no-op，且管理器不得把 `isCapturing` 置真 |
| RQ-8 | 切换分辨率的副作用 | 不重新申请 USB 权限、不中断 WebRTC 会话（沿用现有路径） |

## 2. 实现方案摘要

- `CameraSession` 保存当前采集规格（`CaptureSpec`，默认 1280x720@30）；`startCapturer`、`resumeCapture`、`changeResolution` 统一使用它。
- `resumeCapture()` 返回是否真正恢复；恢复成功时通过 `onCaptureStarted` 回报规格。
- `WebRtcSessionManagerImpl` 维护全局首选规格：`switchResolution` 成功时更新。
- 激活摄像头时先把会话规格重置为默认；采集启动后轮询支持列表（最多约 2 秒），首选受支持则调用 `changeResolution` 应用，否则保持默认。
- `changeResolution` 返回是否执行，管理器仅在执行成功时更新首选。

## 3. 测试用例

### 3.1 自动化单元测试（Kotlin/JUnit）

运行方式：

```powershell
cd pg-kvm-android
.\gradlew.bat :app:testDebugUnitTest --tests "*CaptureSpecTest*" --console=plain
```

| 用例 | 场景 | 期望 |
| --- | --- | --- |
| U-01 | 首选规格与支持列表完全匹配 | 返回对应项 |
| U-02 | 尺寸匹配但最大帧率不同 | 返回该尺寸（保留用户选择的帧率，摄像头以自身帧率范围采集） |
| U-03 | 支持列表为空 | 返回 `null` |
| U-04 | 首选规格不在列表中 | 返回 `null` |
| U-05 | 默认规格常量 | `CaptureSpec.DEFAULT` 为 1280x720@30 |

### 3.2 编译与静态检查

| 用例 | 场景 | 命令/方法 | 期望 |
| --- | --- | --- | --- |
| BT-01 | Kotlin 编译 | `.\gradlew.bat :app:compileDebugKotlin` | 通过 |
| BT-02 | 完整构建 | `.\gradlew.bat :app:assembleDebug` | 通过 |
| BT-03 | 启动路径使用保存规格 | 检查 `CameraSession` | `startCapturer` / `resumeCapture` / `changeResolution` 均使用保存值 |
| BT-04 | 状态回报 | 检查 `onCaptureStarted` 调用点 | 启动、恢复、切换分辨率均回报 |
| BT-05 | 首选更新时机 | 检查管理器 | 仅 `changeResolution` 真正执行时更新首选 |
| BT-06 | 激活时回退策略 | 检查管理器 | 激活先重置默认；支持列表就绪后应用首选，不支持则保持默认 |

### 3.3 真机端到端（需 UVC 摄像头）

| 用例 | 场景 | 期望 |
| --- | --- | --- |
| MAN-01 | 选择 1920x1080@30 → 关闭预览 → 开启预览 | 仍为 1920x1080@30，状态一致 |
| MAN-02 | 选择 1920x1080@30 → 停止拉流 → 重新开始拉流 | 仍为 1920x1080@30，状态一致 |
| MAN-03 | 摄像头 A 选 1920x1080@30 → 切到支持该规格的 B | B 以 1920x1080@30 采集 |
| MAN-04 | 切到不支持该规格的摄像头 C | C 回退默认；切回 A/B 后恢复 1920x1080@30 |
| MAN-05 | 每次切换后查看设备状态 | 分辨率/帧率与实际采集一致 |

模拟器无 UVC 摄像头，MAN 用例必须真机执行。

## 4. 结果记录

自动化与构建结果、真机验证结论记录在 `results.md`。
