# Tasks

## 1. 会话内规格状态

- [ ] 1.1 在 `CameraSession` 中增加当前宽/高/帧率字段（默认 1280x720@30），让 `startCapturer` 使用该字段；编译 `:app:compileDebugKotlin` 通过，且首次采集仍为默认值。
- [ ] 1.2 让 `changeResolution` 更新字段后重启采集，让 `resumeCapture` 使用字段而非默认值；真机验证：切换分辨率 → 关闭预览 → 开启预览，状态仍显示所选规格。
- [ ] 1.3 确保 `onCaptureStarted` 在所有启动路径回报实际规格；验证 `StreamInfo.resolution/frameRate` 与采集规格一致。

## 2. 跨摄像头首选规格

- [ ] 2.1 在 `WebRtcSessionManagerImpl` 中增加首选规格，`switchResolution` 成功后更新；验证切换分辨率后状态更新。
- [ ] 2.2 激活摄像头时读取其支持列表，受支持则应用首选规格，否则保持默认；真机验证：在 A 选 1920x1080 → 切到支持该规格的 B，B 以 1920x1080 启动。
- [ ] 2.3 验证不支持首选规格的摄像头回退默认且不覆盖首选；再切回支持该规格的摄像头时恢复首选。

## 3. 集成验证与文档

- [ ] 3.1 完整构建 `.\gradlew.bat :app:assembleDebug`，按 `uvc-capture` 的“暂停后恢复”“切换分辨率”“切换摄像头”场景逐项验证。
- [ ] 3.2 更新 `pg-kvm-android/CLAUDE.md` 中关于 `resumeCapture` 回退默认值的“易踩的坑”条目，使其反映新行为。

## Workflow follow-up

- 验证完成后，使用 `openspec archive preserve-resolution-selection --skip-specs` 归档本 change。
