# Proposal

## Why

主 spec `uvc-capture` 已要求用户选择的分辨率在暂停/恢复预览、停止/重新开始串流后保持，但当前实现中 `CameraSession.resumeCapture()` 固定使用 1280x720@30，导致用户切换分辨率后一旦暂停预览或停流就会回退，行为与契约不一致。

## What Changes

- 在 `CameraSession` 中保存当前生效的宽、高、帧率；`startCapturer`、`resumeCapture`、`changeResolution` 统一使用保存值。
- 在 `WebRtcSessionManagerImpl` 中维护跨摄像头的首选规格；用户切换分辨率时更新，激活新摄像头时若支持则应用，否则回退默认值。
- 保证 `StreamInfo` 始终反映实际生效的规格。
- 不修改任何 spec：目标行为已写入 `uvc-capture`。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

无（实现与既有 `uvc-capture` 契约对齐，无 spec-level 行为变化）。

## Impact

- 代码：`app/.../stream/sessions/CameraSession.kt`、`app/.../stream/sessions/WebRtcSessionManagerImpl.kt`。
- 无 API、协议或持久化格式变化。
- 验证：编译后按 `uvc-capture` 的“暂停后恢复”和“切换摄像头”场景真机验证。
