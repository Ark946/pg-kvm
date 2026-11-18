# retire-legacy-camera-info-api 测试结果

- 执行日期：2026-11-18
- 环境：Windows 11、Android 模拟器 `Small_Phone`（Android 16 / API 36，`sdk_gphone16k_x86_64`）
- 被测 APK：clean 构建产物 `pg-kvm-android/app/build/outputs/apk/debug/app-debug.apk`

## 1. 调用方检索

命令：

```powershell
git grep -n -E "CameraInfoController|/api/camera/info" -- . ":(exclude)test" ":(exclude)**/build/**"
```

结果：除控制器自身、OpenSpec 规格/变更文档与 `CLAUDE.md` 外，没有前端、Kotlin/Java 调用方。
删除后再次检索 `pg-kvm-android`，仅剩 `CLAUDE.md` 中“已移除”的说明。

结果：✅ ST-01 通过。

## 2. 构建与 kapt 验证

| 用例 | 结果 | 证据 |
| --- | --- | --- |
| BT-01 删除后编译 | ✅ 通过 | `.\gradlew.bat clean :app:assembleDebug` 中的 Kotlin 编译成功 |
| BT-02 clean 构建 | ✅ 通过 | `BUILD SUCCESSFUL in 1m 28s`，124 tasks |
| BT-03 kapt 注册检查 | ✅ 通过 | 检索 `app/build/generated` 无 `CameraInfoController` 或 `/api/camera/info`，输出 `NO_CAMERA_INFO_REFERENCES` |

## 3. 运行时验证

模拟器安装并启动应用后：

### `/api/camera/info`

```http
HTTP/1.1 404 Not Found
Server: AndServer/2.1.9
Content-Length: 45
Content-Type: text/plain;charset=UTF-8

The resource [/api/camera/info] is not found.
```

结果：✅ IT-01 通过，端点不再返回有效摄像头信息。

### `/api/status`

```http
HTTP/1.1 200 OK
Content-Type: application/json;charset=UTF-8

{"cameras":[],"currentCameraName":null,"ports":{"http":8080,"signaling":3000,"relay":3001},"stream":{...},"ble":{...}}
```

结果：✅ IT-02 通过，状态接口及其余字段正常。

## 4. 文档

- `CLAUDE.md` 的关键文件表中删除了 `CameraInfoController.kt` 行。
- “易踩的坑”第 11 条改为“`/api/camera/info` 已移除，设备信息统一从 `/api/status` 获取”。

结果：✅ DOC-01 通过。

## 5. 结论

全部用例通过；`/api/camera/info` 已从路由、实现和 kapt 生成物中移除，`/api/status` 行为不变。
