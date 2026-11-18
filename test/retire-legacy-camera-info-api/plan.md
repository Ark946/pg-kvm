# retire-legacy-camera-info-api 测试计划

## 1. 需求澄清

| 编号 | 澄清项 | 结论 |
| --- | --- | --- |
| RQ-1 | 处置方式 | 删除 `CameraInfoController` 及其注解路由；不保留占位控制器 |
| RQ-2 | 兼容性 | 不提供重定向、410 或兼容响应；客户端改用 `GET /api/status` |
| RQ-3 | 验收标准 | `/api/camera/info` 不再返回有效设备信息；不强制特定 HTTP 状态码 |
| RQ-4 | 其他 API | `/api/status` 及现有控制接口保持不变 |
| RQ-5 | kapt 残留 | 使用 clean build 验证生成的控制器注册不再包含 `CameraInfoController` |
| RQ-6 | 文档 | `pg-kvm-android/CLAUDE.md` 中“遗留接口”描述改为“已移除” |

## 2. 实现方案摘要

- 全仓检索确认无调用方后，删除 `CameraInfoController.kt`。
- 清理构建产物后重新构建，检查 kapt 生成的 `ConfigRegister` 不再注册该控制器。
- 模拟器安装运行：请求 `/api/camera/info` 确认不再可用，请求 `/api/status` 确认正常。

## 3. 测试用例

| 用例 | 场景 | 命令/步骤 | 期望 |
| --- | --- | --- | --- |
| ST-01 | 全仓调用方检索 | `rg -n "CameraInfoController\|/api/camera/info"` | 除控制器文件和文档外无调用方 |
| BT-01 | 删除后 Kotlin 编译 | `.\gradlew.bat :app:compileDebugKotlin` | 通过 |
| BT-02 | clean 构建 | `.\gradlew.bat clean :app:assembleDebug` | 通过 |
| BT-03 | kapt 注册检查 | 检索 `app/build/generated` | 不包含 `CameraInfoController` |
| IT-01 | 遗留端点不可用 | `curl http://<host>:8080/api/camera/info` | 不返回有效摄像头信息 JSON |
| IT-02 | 状态接口正常 | `curl http://<host>:8080/api/status` | 返回包含 cameras/stream/ble 等字段的 JSON |
| DOC-01 | 文档更新 | 检查 `CLAUDE.md` | 不再描述为可用的遗留接口 |

## 4. 结果记录

执行结果记录在 `results.md`。
