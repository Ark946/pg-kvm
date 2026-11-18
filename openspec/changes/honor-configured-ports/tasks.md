# Tasks

## 1. 状态接口暴露端口

- [ ] 1.1 在 `DeviceApiController` 的 `/api/status` 响应中加入 `ports` 对象（`http`、`signaling`、`relay`），取自管理器当前端口；用 `curl`/浏览器验证字段存在且与设置一致。
- [ ] 1.2 验证未初始化会话时仍返回结构化错误，且不因新增字段改变既有响应。

## 2. 网页端使用动态端口

- [ ] 2.1 在 `App.vue` 的 `DeviceStatus` 类型中加入 `ports`，移除模块级 `WS_URL` / `INPUT_WS_URL` 常量；`npm run build` 通过。
- [ ] 2.2 在开始拉流前刷新 `/api/status`，用 `window.location.hostname` 与状态中的端口拼接信令/输入 WS URL；状态不可用或端口缺失时显示错误并阻止连接。
- [ ] 2.3 验证自定义端口：应用内改为 4000/4001 → 重新加载页面 → 开始拉流，确认视频与输入均连接新端口。
- [ ] 2.4 验证默认端口（3000/3001）下行为与改动前一致。

## 3. 集成验证与文档

- [ ] 3.1 执行 `.\gradlew.bat :app:assembleDebug`（含前端打包与 assets 同步），确认构建产物包含新逻辑。
- [ ] 3.2 更新 `pg-kvm-android/CLAUDE.md` 中关于端口写死的描述；确认 `openspec/specs/web-console/spec.md` 已描述目标行为、无需修改。
- [ ] 3.3 真机按 `web-console` 的“自定义端口”场景完成端到端验证。

## Workflow follow-up

- 验证完成后，使用 `openspec archive honor-configured-ports --skip-specs` 归档本 change。
