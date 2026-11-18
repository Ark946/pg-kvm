# honor-configured-ports 测试计划

## 1. 需求澄清

| 编号 | 澄清项 | 结论 |
| --- | --- | --- |
| RQ-1 | 状态接口如何暴露端口 | `GET /api/status` 响应新增 `ports` 对象：`http`、`signaling`、`relay`，取自 `WebRtcSessionManager.getServerPorts()` |
| RQ-2 | 兼容性 | 纯新增字段；`cameras`、`currentCameraName`、`stream`、`ble` 及“会话未就绪”错误响应不变 |
| RQ-3 | 网页端何时读取端口 | 用户点击“开始拉流”时重新请求 `/api/status`，不依赖 2 秒轮询缓存 |
| RQ-4 | WebSocket 地址 | `ws://<window.location.hostname>:<signalingPort>` 与 `ws://<window.location.hostname>:<relayPort>` |
| RQ-5 | 状态/端口不可用时 | 显示明确错误并阻止创建信令与输入连接；禁止回退 3000/3001 |
| RQ-6 | 非法端口 | 非整数、小于 1、大于 65535、缺字段均视为不可用 |
| RQ-7 | 修改端口后的页面行为 | 需要用户重新加载页面；不迁移已建立连接 |
| RQ-8 | `http` 端口用途 | 仅暴露信息；页面本身由该 HTTP 服务提供，连接地址使用当前页面主机名 |

## 2. 实现方案摘要

- 后端：`DeviceApiController.getStatus()` 增加 `ports` 字段，复用 `sm.getServerPorts()`。
- 前端：新增纯函数模块 `frontend/src/ports.ts`（解析与 URL 拼接），`App.vue` 删除模块级固定端口常量，`startConnection()` 先严格拉取状态，失败则显示 i18n 错误并返回。
- 文档：更新 `pg-kvm-android/CLAUDE.md` 中“前端 WS 端口写死”的旧描述。

## 3. 测试用例

### 3.1 自动化单元测试（前端端口解析）

运行方式：

```powershell
node --test test/honor-configured-ports/automated/ports.test.ts
```

| 用例 | 场景 | 输入 | 期望 |
| --- | --- | --- | --- |
| UTC-01 | 默认端口 | `ports = {http:8080,signaling:3000,relay:3001}`，host=`192.168.1.10` | 信令 `ws://192.168.1.10:3000`，输入 `ws://192.168.1.10:3001` |
| UTC-02 | 自定义端口 | `ports = {http:8080,signaling:4000,relay:4001}` | 信令 `ws://...:4000`，输入 `ws://...:4001` |
| UTC-03 | 使用页面主机名 | host=`kvm.local` | URL 中使用 `kvm.local` |
| UTC-04 | 缺少 `ports` | `{}` / `null` / 非对象 | 返回 `null`（阻止连接） |
| UTC-05 | 缺少单个端口 | 缺 `relay` 或缺 `signaling` | 返回 `null` |
| UTC-06 | 非法端口值 | `0`、`65536`、`3.5`、字符串、`null`、`NaN` | 返回 `null` |
| UTC-07 | 端口边界 | `1` 与 `65535` | 正常生成 URL |
| UTC-08 | 状态为 `null`/字符串 | `null`、`"ok"` | 返回 `null` |

### 3.2 静态与构建验证

| 用例 | 场景 | 命令/方法 | 期望 |
| --- | --- | --- | --- |
| BT-01 | 前端类型检查与打包 | `cd frontend; npm run build` | 退出码 0，产物包含新逻辑 |
| BT-02 | Android 完整构建 | `.\gradlew.bat :app:assembleDebug --console=plain` | 构建成功，`assets/web` 同步新前端 |
| BT-03 | 后端代码契约 | 检查 `DeviceApiController` | `ports` 取自 `getServerPorts()`；早退错误分支不变 |
| BT-04 | 文档更新 | 检查 `pg-kvm-android/CLAUDE.md` | 不再声称前端端口写死 3000/3001 |

### 3.3 运行时集成测试

| 用例 | 场景 | 前置 | 步骤 | 期望 |
| --- | --- | --- | --- | --- |
| IT-01 | 状态接口包含端口 | 服务已启动（真机/模拟器） | `curl http://<host>:8080/api/status` | JSON 含 `ports.http/signaling/relay`，与设置一致 |
| IT-02 | 默认端口拉流 | 默认 3000/3001 | 加载页面 → 点击开始拉流 | 信令连 3000，输入连 3001 |
| IT-03 | 自定义端口拉流 | 改为 4000/4001 并重启服务 | 重新加载页面 → 点击开始拉流 | 信令连 4000，输入连 4001，视频与输入正常 |
| IT-04 | 端口字段缺失 | 模拟 `/api/status` 无 `ports` | 点击开始拉流 | 显示错误文案，且不创建 WebSocket |
| IT-05 | 状态接口不可达 | 模拟 `/api/status` 失败 | 点击开始拉流 | 显示错误文案，按钮可重试 |
| IT-06 | 会话未就绪 | HTTP 已启动、会话未注入 | 请求 `/api/status` | 返回 `{"ok":false,"message":"session not ready"}` |

### 3.4 真机端到端

| 用例 | 场景 | 期望 |
| --- | --- | --- |
| MAN-01 | 真机修改信令/输入端口为 4000/4001 后重新加载页面并拉流 | 视频与键鼠输入均通过新端口工作 |
| MAN-02 | 真机默认端口回归 | 与改动前一致 |

## 4. 结果记录

自动化与构建结果记录在 `results.md`；真机用例在硬件可用后补充执行记录。
