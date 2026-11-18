# Tasks

## 1. 移除遗留控制器

- [x] 1.1 全仓检索 `CameraInfoController`、`/api/camera/info`，确认除控制器本身外无调用方；记录检索命令与结果。
- [x] 1.2 删除 `app/src/main/kotlin/com/arkj/compose/server/controller/CameraInfoController.kt`，编译 `:app:compileDebugKotlin` 通过。

## 2. 清理与验证

- [x] 2.1 执行 clean build（或清理 kapt 生成目录后重建），确认生成的控制器注册中不再包含 `CameraInfoController`。
- [x] 2.2 安装运行后请求 `/api/camera/info`，确认不再返回有效设备信息；请求 `/api/status`，确认正常返回。
- [x] 2.3 更新 `pg-kvm-android/CLAUDE.md` 中把 `/api/camera/info` 描述为“遗留接口”的条目，说明其已移除。

## Workflow follow-up

- 验证完成后，使用 `openspec archive retire-legacy-camera-info-api --skip-specs` 归档本 change。
