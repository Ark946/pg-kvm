# Proposal

## Why

主 spec `device-api` 要求移除遗留端点 `/api/camera/info`。当前 `CameraInfoController` 的数据提供者从未注入，响应是默认对象的 `toString()` 而非 JSON，会误导任何尝试调用它的客户端，并让 API 面积与文档不一致。

## What Changes

- 删除 `CameraInfoController` 及其注解路由，使 `/api/camera/info` 不再作为可用端点暴露。
- 不提供替代的兼容端点；设备信息统一通过 `GET /api/status` 获取。
- 不修改任何 spec：移除要求已写入 `device-api`。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

无（实现与既有 `device-api` 契约对齐，无 spec-level 行为变化）。

## Impact

- 代码：删除 `app/.../server/controller/CameraInfoController.kt`。
- API：`/api/camera/info` 不再可用；仓库内无已知调用方。
- kapt 生成的控制器注册会随重建更新；必要时执行 clean build。
- 验证：请求 `/api/camera/info` 不再返回有效设备信息；`/api/status` 正常。
