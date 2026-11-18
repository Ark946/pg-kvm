# Design

## Context

`CameraSession` 目前不保存采集规格：`startCapturer()` 固定 1280x720@30，`resumeCapture()` 也固定使用默认值，只有 `UvcCameraVideoCapture` 内部短暂记住上一次的 width/height/fps。用户通过 `changeResolution()` 选择规格后，一旦暂停预览或停流，恢复时就丢失选择。管理器也没有跨摄像头的首选规格。

约束：USB 权限与摄像头打开是异步的；支持的分辨率列表只有在摄像头打开后才能读取。

## Goals / Non-Goals

**Goals:**

- 暂停/恢复预览、停止/重新开始串流后保持用户选择的规格。
- 切换摄像头后，若新摄像头支持则沿用首选规格，否则回退默认值。
- `StreamInfo` 始终反映实际生效的规格。

**Non-Goals:**

- 跨应用重启持久化规格。
- 自动挑选“最佳”分辨率或改变默认值策略。
- 修改 UVC 采集管线或分辨率切换的底层实现。

## Decisions

1. **在 `CameraSession` 内保存当前规格**（width/height/fps），初始化默认值；`startCapturer`、`resumeCapture`、`changeResolution` 统一读写该字段。
   - 理由：会话本来就是采集生命周期的所有者，暂停/恢复在同一会话内完成，无需引入全局状态。
   - 备选：只依赖 capturer 内部字段——但 `pauseCapture` 后 `resumeCapture` 无法读到外部选择，且语义不清晰。

2. **在 `WebRtcSessionManagerImpl` 维护跨摄像头首选规格**，用户调用 `switchResolution` 时更新。
   - 理由：spec 要求切换摄像头后继续采用；规格属于用户偏好而非单摄像头状态。
   - 备选：按摄像头持久化到 SharedPreferences——超出当前 spec，且增加状态同步复杂度。

3. **激活摄像头后在支持列表中校验首选规格**：摄像头以默认规格启动，读取支持列表；首选规格受支持则调用 `changeResolution` 应用，否则保持默认。
   - 理由：支持列表必须等摄像头打开后才能获取；复用现有 `changeResolution` 路径，避免重新申请 USB 权限。
   - 补充：支持列表对每个尺寸只暴露最大帧率，因此按宽高匹配；应用时仍保留用户选择的帧率，实际采集帧率由摄像头自身范围决定。
   - 备选：把首选规格直接传给首次 `startCapture`——无法在不打开设备的情况下确认是否受支持，失败路径更复杂。

4. **所有实际规格变化统一经 `onCaptureStarted` 回报**，管理器据此更新 `StreamInfo`。
   - 理由：避免 UI 显示的规格与实际采集规格不一致。

## Risks / Trade-offs

- [激活摄像头时可能多一次预览重启] → 仅在首选规格受支持且与默认不同时发生；可接受短暂闪屏，换取跨摄像头一致性。
- [`resumeCapture()` 在 capturer 为空时可能被调用] → 保持 no-op，且管理器不把 `isCapturing` 置真；恢复路径必须先有活跃 capturer。
- [首选规格在设备固件变化后不再受支持] → 每次激活都重新校验支持列表，不支持则回退默认。
- [切换回来时默认规格覆盖了之前的首选] → 只有用户显式切换分辨率才更新首选；设备不支持时只回退本次启动，不覆盖首选。

## Migration Plan

无数据迁移。发布后首次启动使用默认规格；用户选择后按上述规则保持。回滚只需还原代码，不涉及持久化格式。

## Open Questions

无。
