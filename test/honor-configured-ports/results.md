# honor-configured-ports 测试结果

- 执行日期：2026-11-18
- 环境：Windows 11、Node.js v24.21.0、Android 模拟器 `Small_Phone`（Android 16 / API 36，`sdk_gphone16k_x86_64`）
- 被测 APK：`pg-kvm-android/app/build/outputs/apk/debug/app-debug.apk`（104,503,049 字节）
- 说明：本次没有连接物理真机，运行时验证使用 Android 模拟器 + `adb forward` + Playwright 浏览器完成。

## 1. 自动化单元测试

命令：

```powershell
node --test test\honor-configured-ports\automated\ports.test.ts test\honor-configured-ports\automated\device-status.test.ts
```

结果：**14 通过 / 0 失败**。

| 用例 | 结果 | 说明 |
| --- | --- | --- |
| UTC-01 默认端口 3000/3001 | ✅ 通过 | 生成 `ws://192.168.1.10:3000`、`:3001` |
| UTC-02 自定义端口 4000/4001 | ✅ 通过 | 生成 `ws://...:4000`、`:4001` |
| UTC-03 页面主机名 | ✅ 通过 | 使用传入的 `kvm.local` |
| UTC-04 缺少 `ports` | ✅ 通过 | 返回 `null` |
| UTC-05 缺少单个端口 | ✅ 通过 | 返回 `null` |
| UTC-06 非法端口值 | ✅ 通过 | `0`、`65536`、`-1`、`3.5`、字符串、`null`、`undefined`、`NaN`、`Infinity` 均拒绝 |
| UTC-07 端口边界 | ✅ 通过 | `1` 与 `65535` 可用 |
| UTC-08 状态非对象 | ✅ 通过 | `null`、`undefined`、字符串、数字均返回 `null` |
| DST-01 合法状态 | ✅ 通过 | 结构校验返回 `true` |
| DST-02 缺少 `ports` 的旧版状态 | ✅ 通过 | 仍可用于界面展示（连接前再校验端口） |
| DST-03 会话未就绪错误体 | ✅ 通过 | `{ok:false,message}` 被拒绝 |
| DST-04 空对象 | ✅ 通过 | 被拒绝 |
| DST-05 缺少 `stream`/`ble` | ✅ 通过 | 被拒绝 |
| DST-06 非对象 | ✅ 通过 | 被拒绝 |

## 2. 构建验证

| 用例 | 结果 | 证据 |
| --- | --- | --- |
| BT-01 前端打包 | ✅ 通过 | `npm run build`：vite 4.5.14，16 modules transformed，生成 `dist/` |
| BT-02 Android 完整构建 | ✅ 通过 | `.\gradlew.bat :app:assembleDebug --console=plain`：`BUILD SUCCESSFUL in 2m 13s`；最终增量重建也成功 |
| BT-02b assets 同步 | ✅ 通过 | `assets/web/assets/index-*.js` 中可检索到 `65535`、`signaling`、`relay`、`status_ports_unavailable`、`ws://` |
| BT-03 后端契约 | ✅ 通过 | `DeviceApiController.getStatus()` 通过 `sm.getServerPorts()` 构造 `ports`；会话未就绪的早退分支未改动 |
| BT-04 文档更新 | ✅ 通过 | `pg-kvm-android/CLAUDE.md` 已改为“端口来自 `/api/status`”；`openspec/specs/web-console/spec.md` 已含“端口适配”要求，无需修改 |

## 3. 运行时集成测试

模拟器中的端口通过写入与应用相同的持久化文件 `shared_prefs/stream_server_prefs.xml` 配置（`fixtures/` 下提供两份 XML），效果等同于应用内修改端口后重启服务。

### IT-01 状态接口包含端口

默认端口：

```json
{"cameras":[],"currentCameraName":null,"ports":{"http":8080,"signaling":3000,"relay":3001},...}
```

自定义端口：

```json
{"cameras":[],"currentCameraName":null,"ports":{"http":8080,"signaling":4000,"relay":4001},...}
```

结果：✅ 通过。`ports` 与设置一致，既有字段（`cameras`、`currentCameraName`、`stream`、`ble`）保持不变。

### IT-02 默认端口拉流

步骤：加载 `http://127.0.0.1:18080/` → 注入 WebSocket 观测钩子 → 点击「开始拉流」。

观测到的连接：

```text
ws://127.0.0.1:3000
ws://127.0.0.1:3001
```

页面状态：`输入捕获已连接`；信令已建立并进入 `WebRTC 连接中...`。
结果：✅ 通过，行为与改动前一致。

### IT-03 自定义端口拉流

步骤：将信令/输入端口配置为 4000/4001 → 重启应用（日志显示 relay 监听 4001）→ 重新加载页面 → 点击「开始拉流」。

观测到的连接：

```text
ws://127.0.0.1:4000
ws://127.0.0.1:4001
```

页面状态：`输入捕获已连接`；信令已建立并进入 `WebRTC 连接中...`。
结果：✅ 通过。模拟器无 UVC 摄像头，因此只验证信令/输入连接与端口选择；视频画面需真机确认。

### IT-04 端口字段缺失

步骤：页面加载后替换 `window.fetch`，令 `/api/status` 返回 `{}` → 点击「开始拉流」。

结果：✅ 通过。

- 未创建任何 WebSocket（`urls: []`）。
- 状态文案：`串流：无法获取设备端口信息，请确认服务已启动后重试`。
- 「开始拉流」按钮仍存在，可重试。

### IT-05 状态接口不可达

步骤：令 `/api/status` 请求抛出 `TypeError: Failed to fetch` → 点击「开始拉流」。

结果：✅ 通过。

- 未创建任何 WebSocket（`urls: []`）。
- 显示相同的错误文案。
- 页面仍可操作，可重试。

### IT-06 会话未就绪

结果：⚠️ 仅代码审查。`getStatus()` 的 `sessionManager ?: return error("session not ready")` 早退分支未被本次修改触碰；由于 HTTP 服务与会话同时启动，无法通过 HTTP 在会话未就绪时触发该分支。返回结构由现有代码保证为 `{"ok":false,"message":"session not ready"}`。

## 4. 真机端到端

| 用例 | 状态 | 说明 |
| --- | --- | --- |
| MAN-01 真机自定义端口 4000/4001 拉流并验证键鼠输入 | ✅ 通过 | 用户于 2026-11-18 完成真机验证 |
| MAN-02 真机默认端口回归 | ✅ 通过 | 用户于 2026-11-18 完成真机验证 |

真机确认视频画面、UVC 采集与 BLE 键鼠链路在默认及自定义端口下均正常。

## 5. 测试过程中发现的问题

1. **无效状态响应导致页面不可操作（已在本变更修复）**
   - 现象：`/api/status` 返回 `{ok:false}` 或 `{}` 时，轮询直接写入 `deviceStatus`，模板访问 `stream`/`ble` 抛错，页面内容消失，用户无法点击重试。
   - 处理：新增 `device-status.ts` 的 `isDeviceStatus()` 结构校验；轮询只接受含 `cameras`/`stream`/`ble` 的响应。对应任务 2.5，并补充 DST-01~06 测试。
2. **`vue-tsc` 1.8.5 与当前 TypeScript/Node 不兼容（未修复，非阻塞）**
   - 现象：`npx vue-tsc --noEmit` 报 `Search string not found: "/supportedTSExtensions = .*(?=;)/"`。
   - 影响：`npm run build`（vite）正常；仅独立类型检查工具不可用。建议后续单独升级 `vue-tsc`，本次未纳入变更范围。

## 6. 结论

- 本变更的代码、构建与运行时端口行为已验证通过。
- 自动化用例 14/14 通过；模拟器集成用例 IT-01~IT-05 通过；IT-06 由代码审查覆盖。
- 真机用例 MAN-01/02 由用户验证通过（2026-11-18），全部 10 项任务完成。
