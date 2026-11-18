# CLAUDE.md

本文件为 Claude Code 在 `pg-kvm-android` 中工作时的项目指引。系统的行为契约见仓库根目录的
`openspec/specs/`，进行中的变更见 `openspec/changes/`。

## 项目定位

pg-kvm 是用「Android 手机 + UVC 采集卡 + ESP32-S3」实现的低成本 IP-KVM。Android 应用负责三件事：

1. 通过 USB-OTG 采集 UVC 摄像头（通常接目标机的 HDMI 采集卡）画面，经 WebRTC 推给浏览器；
2. 在应用内跑三个服务器：HTTP 静态页 + REST API、WebRTC 信令、键鼠输入中继；
3. 把浏览器发来的键鼠事件经 BLE 发给 ESP32-S3，由固件以 USB HID 注入目标机。

目标机（被控电脑）不需要安装任何软件。

## 常用命令（Windows / PowerShell）

```powershell
# 完整构建 Debug APK（会先自动打包前端并同步到 assets/web）
.\gradlew.bat :app:assembleDebug --console=plain

# 仅编译 Kotlin / Java，快速检查
.\gradlew.bat :app:compileDebugKotlin :app:compileDebugJavaWithJavac --console=plain

# 仅重建 libuvc native（改了 .cpp/.h 时）
.\gradlew.bat :libuvc:externalNativeBuildDebug --console=plain

# 单独构建前端（通常不需要，preBuild 会自动做）
cd frontend; npm run build; cd ..

# 代码格式（ktlint + license header）
# 注意：会给缺少 Stream 版权头的文件批量补头，可能产生很大 diff，按需使用
.\gradlew.bat spotlessApply

# 安装 / 看日志
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb logcat -s SignalingServer InputRelayServer BleConnection UvcCameraVideoCapture CameraHttpServer
```

Git Bash 下也可用 `./gradlew ...`。项目没有单元测试，验证方式是编译 + 真机运行。

### 构建前置条件

- JDK 17（Gradle 8.9 / AGP 8.3.2）。
- Android SDK 35、NDK `30.0.14904198`、CMake `3.10.2`。
- Node.js + npm（`app/build.gradle.kts` 的 `preBuild` 会调用 `npm run build`）。
- `local.properties` 必须包含以下键（**缺少 `SIGNALING_SERVER_IP_ADDRESS` 会在配置阶段直接抛异常**，
  尽管代码里目前并未使用它）：
  ```properties
  sdk.dir=...
  ndk.dir=...
  SIGNALING_SERVER_IP_ADDRESS="ws://192.168.1.10:3000"
  ```
- Release 签名读取根目录 `keystore.properties`（`storeFile/storePassword/keyAlias/keyPassword`，
  已被 `.gitignore` 忽略）；缺失时 release 构建不可用。

## 模块结构

```
pg-kvm-android/
├── app/            # 主应用（Compose UI、WebRTC、三个服务器、BLE、UVC 封装）
├── frontend/       # Vue 3 + TS + Vite 网页端（构建产物进 app/src/main/assets/web）
├── libuvc/         # libusb + libjpeg-turbo + libuvc + JNI（UVCCamera）
├── libausbc/       # 上游 AndroidUSBCamera 库；本应用主要只用 USBMonitor/CameraUtils
├── libnative/      # lame mp3 / YUV 工具；当前应用未直接使用
└── buildSrc/       # Configurations：compileSdk 35 / minSdk 23 / targetSdk 35
```

依赖版本集中在 `gradle/libs.versions.toml`：Kotlin 2.1.0、Compose 1.7.6、
`stream-webrtc-android` 1.3.6、`Java-WebSocket` 1.5.7、AndServer 2.1.9、coroutines 1.9.0。

## 关键文件地图

| 文件 | 职责 |
| --- | --- |
| `app/.../compose/MainActivity.kt` | 唯一 Activity；Compose 主界面、预览 SurfaceView、BLE/分辨率/端口对话框 |
| `stream/sessions/WebRtcSessionManager.kt` | 对 UI 暴露的接口（全部状态是 `StateFlow`） |
| `stream/sessions/WebRtcSessionManagerImpl.kt` | **总协调器**：全局 helper、摄像头会话表、三个服务器的启停、端口持久化、BLE 重连 |
| `stream/sessions/CameraSession.kt` | 每摄像头会话：capturer + 配对 BLE 连接；dormant/active 生命周期 |
| `java/com/arkj/video/UvcCameraHelper.java` | 全局 USBMonitor：枚举 UVC、插拔监听、USB 权限中介（异步回 `openControlBlock`） |
| `java/com/arkj/video/capture/UvcCameraVideoCapture.java` | 自定义 `org.webrtc.VideoCapturer`；MJPEG/YUYV → I420 零拷贝喂 WebRTC |
| `stream/SignalingServer.kt` | WebSocket 信令（默认 3000）；每个浏览器一个 PeerConnection，Android 为 answerer |
| `stream/peer/StreamPeerConnection*.kt` | WebRTC PeerConnection 封装（UNIFIED_PLAN、硬件编码 + 软编回退） |
| `stream/input/InputRelayServer.kt` | WebSocket 输入中继（默认 3001）；JSON 键鼠事件路由到活跃 BLE |
| `stream/input/BleHelper.kt` | 全局蓝牙适配器 + BLE 扫描（只扫 `KBBridge-ESP32S3`） |
| `stream/input/BleConnection.kt` | 每设备 GATT 连接；协议 v2 组帧、写队列、HID 发送 |
| `stream/input/BlePairingStore.kt` | 「摄像头 ID ↔ BLE 地址」持久化 |
| `stream/input/HID.kt` | USB HID keycode / 修饰键 / 字符映射表 |
| `server/WebStreamPageServer.kt` | AndServer HTTP 服务（默认 8080），托管 `assets/web` |
| `server/controller/DeviceApiController.kt` | 前端侧边卡片调用的 REST API |
| `frontend/src/App.vue` | 网页端全部逻辑：WS/WebRTC、全屏键鼠捕获、状态轮询、侧边卡片 |
| `frontend/src/i18n.ts` | 自研极简中英文案 |

## 核心不变量（改代码前务必确认）

- **全局只有一个 WebRTC 管线**：`VideoSource` / `VideoTrack` / `SurfaceTextureHelper` 在
  `onSessionScreenReady()` 创建一次；切换摄像头只热替换 `CameraSession.capturer`，不要重建 track。
- **全局资源 vs 会话资源**：`UvcCameraHelper`（USBMonitor）、`BleHelper`（BluetoothAdapter）、
  `BlePairingStore`、`InputRelayServer` 全局唯一；`UvcCameraVideoCapture` 和 `BleConnection`
  归各自的 `CameraSession` 所有。同一时刻只有一个活跃会话持有资源。
- **每个摄像头最多配对 1 个 ESP32**；配对关系持久化，切换/重启后按 `cameraId` 自动回连。
- **服务器启动必须离开主线程**：`SignalingServer.start()` / `InputRelayServer.start()` 会阻塞
  等待端口绑定结果；`WebStreamPageServer.start()` 也需要时间。管理器用
  `sessionManagerScope.launch` 包裹。重启前必须先 `stop()` 释放端口（内部已带 2s join）。
- **BLE 协议必须与 ESPRemoteControl 固件一致**：service/characteristic UUID、0xAA/0x01 头、
  opcode 都不能单方面改；改一端必须同步改另一端。
- **帧回调必须恰好归还一次**：`UvcCameraVideoCapture` 的 I420 包装使用 native 帧池内存，
  所有退出路径都要通过 release callback 调用 `UVCCamera.nativeReleaseFrame(framePtr)`。
- **前端 WS 端口来自设备状态**：界面点击「开始拉流」时会重新请求 `/api/status`，用其中的
  `ports.signaling` / `ports.relay` 与当前页面主机名拼接 WS 地址；端口缺失或状态不可用时
  拒绝连接并提示，不回退默认端口。修改端口后需要重新加载网页。
- **输入写队列语义**：按键类事件保序不丢；鼠标移动/滚轮可丢；队列上限 128；
  单包最多重试 5 次（间隔 4ms）。
- **仅第一个输入客户端有效**：`InputRelayServer` 只处理 `activeInputClient` 的消息，
  断开时释放所有按下的键和修饰键。

## 编码约定

- Kotlin 2 空格缩进（spotless/ktlint 配置）；Java 沿用各文件现有缩进（app 内为 2 空格）；
  注释以中文为主，标识符用英文。
- 状态统一用 `StateFlow` 向 UI 暴露，Compose 侧 `collectAsState()`；回调用普通函数字段
  （如 `onCaptureStarted`、`onUnexpectedDisconnect`）。
- 没有 DI 框架：`WebRtcSessionManager` 通过 `LocalWebRtcSessionManager`（CompositionLocal）注入；
  AndServer 控制器由反射创建，只能通过 `companion object` 静态字段取管理器
  （`DeviceApiController.sessionManager`）。
- 新增 AndServer 控制器：`@CrossOrigin` + `@RestController` + 注解方法，**返回手工构造的 JSON
  字符串**（项目未注册 MessageConverter，直接返回对象会变成 `toString()`）。新增/修改后需要
  kapt 重新生成，正常 rebuild 即可。
- 资源文案同时加到 `res/values/strings.xml` 和 `res/values-en/strings.xml`；网页端文案加在
  `frontend/src/i18n.ts`。
- 日志：app Kotlin 用 `io.getstream.log.taggedLogger`；input/native 相关用 `android.util.Log`。
- 不要手改 `app/src/main/assets/web/`（构建生成、已 gitignore）；改 `frontend/src`。
- `libausbc` / `libuvc` / `libnative` 是上游库，非必要不要改；`libuvc` 的帧池/`nativeReleaseFrame`
  是项目定制点，改它要同时看 Java 侧的释放路径。

## 常见修改入口

| 想做什么 | 改哪里 |
| --- | --- |
| 新增 REST 接口 | `server/controller/DeviceApiController.kt` + `frontend/src/App.vue` 的 `fetch` 调用 |
| 新增键鼠消息类型 | `frontend/src/App.vue` → `InputRelayServer.handleInputMessage` → `BleConnection`/`HID` → ESP32 固件 |
| 新增 BLE 指令 | `BleConnection` 的 opcode 常量与 send 方法；同步固件 |
| 改默认分辨率/帧率 | `CameraSession.DEFAULT_WIDTH/HEIGHT/FPS`（注意 `resumeCapture()` 也用它） |
| 改服务器端口 | `WebRtcSessionManagerImpl` 的 `DEFAULT_*_PORT`；网页端无需改代码（从 `/api/status` 的 `ports` 读取） |
| 改摄像头枚举规则 | `UvcCameraHelper.isUvcDevice`（`CameraUtils.isUsbCamera` / `isFilterDevice`） |
| 加设备过滤白名单 | `libuvc/src/main/java/com/jiangdg/usb/DeviceFilter.java` / `CameraUtils` |

## 易踩的坑

1. **`app/src/main/assets/web/` 是生成物**：`preBuild` 会 `npm run build` 再 `Sync` 覆盖它；
   手改会在下次构建被清掉。
2. **applicationId / namespace 仍是上游的** `io.getstream.webrtc.sample.compose`，所以资源引用是
   `io.getstream.webrtc.sample.compose.R`。不要按 `com.arkj` 去找 R 类。
3. **AndServer kapt 依赖**：`javax.activation:activation:1.1.1` 必须显式加到 kapt classpath，
   否则 ControllerProcessor 加载失败、API 全部 404（`app/build.gradle.kts` 已处理）。
4. **`local.properties` 的 `SIGNALING_SERVER_IP_ADDRESS`** 在配置阶段被读取，缺失直接构建失败；
   它目前没有被任何运行时代码使用。
5. **app 的 `jvmTarget = 17`**，根 `build.gradle.kts` 给所有子项目设的是 1.8；改模块编译配置时
   别把 app 降回 1.8。
6. **相机权限决定服务器是否启动**：`onSessionScreenReady()` 由 CAMERA 权限授予触发；
   即使 UVC 走的是 USB 权限，没有 CAMERA 权限整套服务也不会启动。
7. **BLE 权限分代**：Android 12+ 用 `BLUETOOTH_SCAN/CONNECT`；旧版用 `BLUETOOTH(_ADMIN)` +
   `ACCESS_FINE_LOCATION`（`PermissionUtils.BLE_PERMISSIONS` 已按 SDK 分支）。
8. **没有前台 Service**：服务器随 Activity 存活；应用被杀/部分后台清理会导致服务停止。
9. **分辨率切换会重启预览**：`changeResolution()` = `pauseCapture()` + 新尺寸
   `startCapture()`；而 `resumeCapture()` 固定使用 1280x720@30，暂停预览后再恢复会丢失用户选择的分辨率。
10. **`StreamInfo.isStreaming` 表示「服务器已启动」而非「有浏览器在看」**；客户端列表看
    `clientAddresses`。
11. **`/api/camera/info` 已移除**：该遗留端点不再暴露；设备信息统一从 `/api/status` 获取，
    不要再新增兼容端点或重定向。
12. **BLE 未就绪时输入会被静默丢弃**（`InputRelayServer` 只打日志），网页端不会收到错误。
13. **鼠标位移单包 clamp 到 ±127**，高 DPI/快速移动可能被截断。

## 验证流程

1. `.\gradlew.bat :app:assembleDebug` 通过。
2. 真机安装，授予相机权限；日志应出现 HTTP / signaling / relay 三个服务启动成功。
3. 手机与电脑同一局域网，浏览器打开 `http://<手机IP>:8080`。
4. 点击「开始拉流」，视频出现后进入全屏，键鼠事件才会被捕获。
5. BLE 面板连接 `KBBridge-ESP32S3` 后，目标机应能收到键鼠输入。
6. 修改前端后确认 `assets/web` 里是新产物（构建日志中有 `buildFrontend` / `syncWebAssets`）。
