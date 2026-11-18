# Spec Delta

## Purpose

通过 HTTP 向网页端和局域网工具提供设备状态查询、摄像头与 BLE 控制接口，并托管网页端静态资源，使零安装的浏览器客户端能够发现并操作设备。

## ADDED Requirements

### Requirement: 静态站点托管

设备 SHALL 在 HTTP 服务的根路径提供网页端入口页面及其静态资源。入口页面加载后 SHALL 能获取同一服务下的 API。

#### Scenario: 打开页面

- **WHEN** 浏览器访问设备 HTTP 服务的根路径
- **THEN** 服务器返回网页端入口页面，页面资源可正常加载

### Requirement: 状态查询接口

`GET /api/status` SHALL 返回包含 `cameras`、`currentCameraName`、`stream`、`ble` 四个字段的 JSON。`cameras` 每项 SHALL 包含 `id`、`name`、`current`；`stream` SHALL 包含分辨率、帧率、编码、串流状态、服务地址和客户端地址；`ble` SHALL 包含状态、就绪标志、扫描状态、已连接设备名和输入客户端数。

#### Scenario: 正常查询

- **WHEN** 会话已就绪且客户端请求 `/api/status`
- **THEN** 返回上述结构的 JSON，字段值与当前设备状态一致

#### Scenario: 无摄像头

- **GIVEN** 没有接入 UVC 摄像头
- **WHEN** 客户端请求 `/api/status`
- **THEN** `cameras` 为空数组，`currentCameraName` 为 `null`

### Requirement: 分辨率查询接口

`GET /api/resolutions` SHALL 返回 `resolutions` 数组，每项包含 `width`、`height`、`fps`，并按像素总数降序排列。

#### Scenario: 查询支持规格

- **WHEN** 活跃摄像头支持多个分辨率
- **THEN** 返回的列表按 1920x1080 在 1280x720 之前的方式降序排列

### Requirement: BLE 设备查询接口

`GET /api/ble/devices` SHALL 返回 `isScanning` 与 `devices`。`devices` 每项 SHALL 包含 `name`、`address`、`rssi`。

#### Scenario: 扫描中查询

- **WHEN** BLE 扫描正在进行且已有发现结果
- **THEN** 返回 `isScanning: true` 和当前设备列表

### Requirement: 控制接口

以下接口 SHALL 执行对应操作：`POST /api/camera/switch?cameraId=`、`POST /api/resolution/switch?width=&height=&fps=`、`POST /api/ble/scan/start`、`POST /api/ble/scan/stop`、`POST /api/ble/connect?address=`、`POST /api/ble/disconnect`。成功 SHALL 返回 `{"ok":true}`；失败 SHALL 返回 `{"ok":false,"message":"..."}`。

#### Scenario: 切换摄像头

- **WHEN** 客户端调用摄像头切换接口并传入有效的 `cameraId`
- **THEN** 设备切换活跃摄像头并返回 `ok: true`

#### Scenario: 无效参数

- **WHEN** 客户端缺少必需的查询参数或传入无效值
- **THEN** 接口返回 `ok: false` 与可读的错误信息，设备状态不变

### Requirement: 跨域访问

API SHALL 允许来自局域网浏览器的跨域请求，使不同来源的网页端仍可调用状态与控制接口。

#### Scenario: 跨域调用

- **WHEN** 浏览器从非设备来源发起 API 请求
- **THEN** 响应包含允许跨域访问的头部，请求可被读取

### Requirement: 会话未就绪时的错误

会话尚未初始化时，所有 `/api/*` 请求 SHALL 返回结构化 JSON 错误，MUST NOT 返回非 JSON 内容或导致进程崩溃。

#### Scenario: 启动早期请求

- **GIVEN** HTTP 服务已启动但摄像头会话尚未初始化
- **WHEN** 客户端请求 `/api/status`
- **THEN** 返回 `{"ok":false,"message":"session not ready"}`

### Requirement: 遗留端点移除

`/api/camera/info` MUST NOT 继续作为可用端点暴露；客户端 SHALL 使用 `/api/status` 获取设备信息。

#### Scenario: 请求遗留端点

- **WHEN** 客户端请求 `/api/camera/info`
- **THEN** 设备不返回有效的摄像头信息 JSON，客户端改用 `/api/status`
