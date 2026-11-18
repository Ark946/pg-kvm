# Proposal

## Why

pg-kvm 已经端到端可用，但“系统应当表现出什么行为”只散落在代码、README 和口头约定里：
跨端契约（浏览器 WebSocket 协议、BLE 协议 v2、REST JSON 形状、摄像头会话生命周期）
没有一份可评审、可追溯的基准。任何重构或功能改动都可能在不被察觉的情况下破坏另一端
（尤其是与 ESPRemoteControl 固件的兼容性）。现在需要建立 to-be 行为契约，让后续每个
change 都有明确的 delta 对象。

## What Changes

- 建立 6 个 capability 的 to-be specs，描述目标行为而非当前实现细节。
- 明确 v1 产品边界：局域网、单用户、无鉴权/TLS、无音频、无 TURN；只覆盖应用侧行为
  以及与 ESP32 固件的外部契约，固件实现不在范围内。
- 将以下目标行为写入契约（它们与当前代码不一致，后续需分别开 change 对齐）：
  - 分辨率选择在暂停预览、停止串流和摄像头切换后保持；
  - 网页端在用户自定义 HTTP / 信令 / 输入端口下仍能连接；
  - 移除或补全不可用的遗留端点 `/api/camera/info`。
- 不修改任何代码；本次只创建 OpenSpec 变更工件（proposal + specs）。
- 已知缺口不写成“缺陷需求”，而是作为契约的未满足项，后续单独提案修复。

## Capabilities

### New Capabilities

- `uvc-capture`: UVC 设备枚举、USB 权限、采集与本地预览、分辨率选择与保持、热插拔和摄像头会话切换。
- `webrtc-streaming`: 信令服务、每浏览器独立的 PeerConnection、offer/answer/ICE、视频轨生命周期。
- `remote-input`: 浏览器输入 WebSocket 协议、活跃客户端规则、BLE 扫描/配对/重连、协议 v2 与 HID 映射。
- `web-console`: 浏览器端页面行为，包括拉流控制、全屏键鼠捕获、状态展示、多语言与端口适配。
- `device-api`: HTTP 静态站点托管与 REST 设备管理接口的路径、参数和响应契约。
- `service-lifecycle`: 三个内置服务的启停、端口配置与持久化、失败上报、权限门禁和进程生命周期边界。

### Modified Capabilities

无（当前 `openspec/specs/` 为空，本次全部为新增 capability）。

## Impact

- 仅新增 `openspec/changes/establish-product-contract/` 下的 proposal 与 specs 工件，不触碰代码。
- 契约覆盖 `pg-kvm-android/` 的 app、frontend 及相关 native 边界；ESP32 固件只作为外部依赖出现。
- 后续实现需新增独立 change：
  - 分辨率的暂停/恢复保持；
  - 网页端端口可配置；
  - 遗留 `/api/camera/info` 的处置；
  - （可选）前台服务与后台存活策略。
- 构建与依赖不变；不涉及数据库、账号或云端服务。
