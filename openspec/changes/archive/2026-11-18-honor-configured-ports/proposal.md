# Proposal

## Why

主 spec `web-console` 已要求网页端使用设备当前生效的信令与输入端口，但 `frontend/src/App.vue` 把 3000 和 3001 写死。用户在应用内修改端口后，网页端无法连接，行为与契约不一致。

## What Changes

- 在 `GET /api/status` 响应中增加当前服务端口字段（HTTP、信令、输入），作为向后兼容的附加字段。
- 网页端在开始拉流前读取状态中的端口，按 `ws://<host>:<signalingPort>` 与 `ws://<host>:<relayPort>` 建立连接，不再使用固定值。
- 状态不可用或端口缺失时，网页端显示明确错误，而不是回退到固定端口。
- 轮询时忽略结构无效的状态响应（如会话未就绪的 `{ok:false}`），避免页面渲染报错而失去可操作性。
- 不修改任何 spec：目标行为已写入 `web-console`。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

无（`/api/status` 只增加可选字段，既有字段与语义不变；实现与 `web-console` 契约对齐）。

## Impact

- 代码：`app/.../server/controller/DeviceApiController.kt`、`frontend/src/App.vue`。
- API：`/api/status` 响应新增端口字段，向后兼容。
- 无 BLE、WebRTC 信令消息格式变化。
- 验证：修改端口后重新加载页面，确认视频与输入均使用新端口。
