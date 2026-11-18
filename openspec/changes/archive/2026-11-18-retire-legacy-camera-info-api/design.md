# Design

## Context

`CameraInfoController` 通过 AndServer 注解暴露 `GET /api/camera/info`，但 `cameraInfoProvider` 从未注入，返回值是默认 `CameraInfo` 数据类的 `toString()`，不是合法 JSON。仓库内没有前端或其他调用方；设备信息已由 `/api/status` 完整提供。主 spec `device-api` 要求该遗留端点不再暴露。

## Goals / Non-Goals

**Goals:**

- 移除 `/api/camera/info` 的路由与实现。
- 保证重建后没有残留的控制器注册。
- 保持 `/api/status` 及其他 API 不变。

**Non-Goals:**

- 重写或迁移该端点的数据。
- 为旧客户端保留兼容层、重定向或 410 响应。
- 调整 AndServer 的 kapt 配置。

## Decisions

1. **直接删除 `CameraInfoController.kt`**，不保留返回错误的占位控制器。
   - 理由：端点没有调用方，删除是最小且最诚实的实现；保留占位会继续扩大 API 面积。
   - 备选：返回 `410 Gone` 并说明替代接口——对内部未使用端点属于过度设计。

2. **不把 `/api/camera/info` 重定向到 `/api/status`**。
   - 理由：两者响应结构不同，重定向会让客户端误以为拿到了兼容数据。

3. **通过 clean build 验证 kapt 生成的控制器注册已消失**。
   - 理由：注解处理器生成的注册代码可能被增量构建缓存；clean 构建能排除残留。
   - 备选：只做增量构建——可能留下旧路由，导致“看似删除但仍可访问”。

4. **以“端点不再返回有效设备信息”为验收标准**，不强制要求特定 HTTP 状态码。
   - 理由：AndServer 对未匹配路由的默认行为不是本变更的契约；spec 只要求该端点不再可用。

## Risks / Trade-offs

- [仓库外存在未知调用方] → 接受；该端点从未返回合法 JSON，实际不可用，继续保留只会延长误导。
- [增量构建残留路由] → 用 clean build 验证，必要时清理 kapt 生成目录。
- [删除后有人误以为设备信息接口消失] → 在 proposal/README 中明确替代接口为 `/api/status`，并在验证步骤中检查。

## Migration Plan

删除代码并重新构建；无数据迁移。回滚即恢复被删除的控制器文件。客户端迁移路径：改用 `GET /api/status`。

## Open Questions

无。
