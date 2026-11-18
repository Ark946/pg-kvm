# remote-input Specification

## Purpose

把浏览器发来的键盘与鼠标事件转换为 BLE HID 指令，交由 ESP32-S3 以标准 USB 键鼠注入目标机，并管理设备配对、重连与发送可靠性。

## Requirements

### Requirement: 输入 WebSocket 服务

应用 SHALL 在配置的输入端口上提供 WebSocket 输入服务并接收 JSON 事件。非法 JSON、缺少 `type`、未知 `type` 或缺少必需字段的消息 MUST 被忽略，且 MUST NOT 影响服务运行。

#### Scenario: 合法输入事件

- **WHEN** 活跃客户端发送合法的 `key-down` 或 `mouse-move` 事件
- **THEN** 应用按事件语义处理并转发到当前 BLE 连接

#### Scenario: 非法消息

- **WHEN** 客户端发送非法 JSON、没有 `type` 或未知 `type`
- **THEN** 应用忽略该消息，服务保持运行

### Requirement: 活跃输入客户端

同一时刻 SHALL 只有一个活跃输入客户端，只有活跃客户端的事件会被执行。活跃客户端断开时，应用 SHALL 释放所有已按下按键与修饰键，并在还有其他客户端时提升一个为活跃客户端。

#### Scenario: 第二个客户端接入

- **GIVEN** 已存在活跃输入客户端
- **WHEN** 第二个客户端连接并发送事件
- **THEN** 第二个客户端的事件被忽略，活跃客户端不变

#### Scenario: 活跃客户端断开

- **GIVEN** 用户正按住 Shift 和某个按键
- **WHEN** 活跃客户端断开
- **THEN** 应用发送抬起事件并清零修饰键，避免目标机出现卡键

### Requirement: 键盘事件映射

应用 SHALL 将浏览器键盘事件映射为 USB HID Usage ID，并维护修饰键状态；无法映射的按键 MUST 被忽略。按下与抬起 SHALL 成对处理，修饰键按下/抬起 SHALL 只改变修饰键状态。

#### Scenario: 普通按键

- **WHEN** 活跃客户端发送 `key-down` 后发送 `key-up`
- **THEN** 目标机收到相同 keycode 的按下与抬起

#### Scenario: 修饰键组合

- **GIVEN** 活跃客户端已发送 Control 与 Shift 的按下事件
- **WHEN** 发送按键 A 的按下事件
- **THEN** 该按键以 Control+Shift 修饰状态发送

#### Scenario: 不可映射按键

- **WHEN** 收到无法映射为 HID keycode 的按键
- **THEN** 应用忽略该事件，不发送任何 BLE 指令

### Requirement: HID 键码与掩码

按键 SHALL 使用 USB HID Usage ID 表示。修饰键掩码 SHALL 为：0x01 左 Ctrl、0x02 左 Shift、0x04 左 Alt、0x08 左 GUI、0x10 右 Ctrl、0x20 右 Shift、0x40 右 Alt、0x80 右 GUI。鼠标按键掩码 SHALL 为：0x01 左键、0x02 右键、0x04 中键。

#### Scenario: 组合修饰键

- **WHEN** 用户同时按下左 Ctrl 与左 Shift，再按下 A
- **THEN** 修饰键掩码为 0x03，按键帧携带 A 的 Usage ID

#### Scenario: 鼠标按键

- **WHEN** 用户按下鼠标右键
- **THEN** 发送的按键掩码为 0x02

### Requirement: 鼠标事件映射

应用 SHALL 支持相对移动、滚轮以及左/中/右键的按下、抬起和点击。单次相对位移或滚动量超出 -128..127 时 SHALL 截断到边界值。

#### Scenario: 相对移动

- **WHEN** 活跃客户端发送 `mouse-move` 且 dx/dy 在范围内
- **THEN** 目标机收到对应的相对位移

#### Scenario: 位移截断

- **WHEN** 单次移动的 dx 为 300
- **THEN** 实际发送的 dx 为 127

#### Scenario: 滚轮

- **WHEN** 活跃客户端发送 `mouse-scroll`
- **THEN** 目标机收到对应的滚动量

### Requirement: BLE 发现与配对

应用 SHALL 扫描名称为 `KBBridge-ESP32S3` 的 BLE 设备。配对关系 SHALL 以摄像头为单位持久化；激活摄像头时若存在配对记录 SHALL 自动连接；用户显式断开 SHALL 清除该摄像头的配对记录。

#### Scenario: 扫描并连接

- **WHEN** 用户打开 BLE 扫描并选择一个扫描到的设备
- **THEN** 应用停止扫描、保存该摄像头与设备的配对关系并发起连接

#### Scenario: 激活已配对摄像头

- **GIVEN** 某摄像头存在配对记录
- **WHEN** 该摄像头成为活跃摄像头
- **THEN** 应用使用记录中的地址自动回连

#### Scenario: 显式断开

- **WHEN** 用户点击断开 BLE
- **THEN** 应用断开连接并清除当前摄像头的配对记录

### Requirement: 意外断开重连

BLE 意外断开时，应用 SHALL 仅在活跃摄像头上自动重连，最多尝试 3 次，间隔 2 秒。连接就绪后 SHALL 重置重试计数；主动断开或切换摄像头 MUST NOT 触发自动重连。

#### Scenario: 意外断开

- **GIVEN** 活跃摄像头已配对且 BLE 就绪
- **WHEN** BLE 连接意外丢失
- **THEN** 应用在 2 秒后重试，最多 3 次

#### Scenario: 重连成功

- **WHEN** 某次重连成功并完成服务发现
- **THEN** 重试计数归零，输入恢复可用

#### Scenario: 主动断开

- **WHEN** 用户主动断开 BLE
- **THEN** 应用不发起自动重连

### Requirement: BLE 协议兼容性

BLE 通信 SHALL 使用 Service UUID `2D2A0001-8A5A-4E76-A2E3-1E57D9A1B001` 与 Write Characteristic UUID `2D2A0002-8A5A-4E76-A2E3-1E57D9A1B001`，并采用协议 v2：每个写包以 `0xAA 0x01` 开头，后跟一个或多个 `[cmd][len][payload]` 帧。opcode 语义 MUST 与 ESPRemoteControl 固件兼容。

#### Scenario: 帧格式

- **WHEN** 应用发送单个按键抬起指令
- **THEN** 写包为 `0xAA 0x01 0x03 0x01 <keycode>`

#### Scenario: 多帧打包

- **WHEN** 应用一次发送多个字符敲击
- **THEN** 多个帧被打包进同一写包，且不超过协商的 MTU 容量

#### Scenario: 协议不匹配

- **GIVEN** 对端固件不实现协议 v2
- **WHEN** 应用发送输入指令
- **THEN** 目标机不会收到有效输入，应用不将其视为成功

### Requirement: 发送队列与重试

BLE 写操作 SHALL 串行执行。按键类事件 MUST NOT 被主动丢弃；队列达到上限时 SHALL 优先丢弃旧的鼠标移动/滚轮事件。单包写失败 SHALL 最多重试 5 次后丢弃，避免队头阻塞；连接断开时 SHALL 清空队列。

#### Scenario: 队列饱和

- **GIVEN** 写队列已满且包含按键与鼠标移动事件
- **WHEN** 新事件入队
- **THEN** 应用优先丢弃旧的鼠标移动/滚轮事件，保留按键事件

#### Scenario: 写失败

- **WHEN** 某次 BLE 写连续失败 5 次
- **THEN** 应用丢弃该包并继续处理后续包

#### Scenario: 连接断开

- **WHEN** BLE 连接断开
- **THEN** 待发送队列被清空

### Requirement: 未就绪时的输入处理

没有就绪的 BLE 连接时，应用 SHALL 丢弃输入事件并保持服务可用。设备状态 SHALL 暴露 BLE 是否就绪，供网页端提示。

#### Scenario: BLE 未连接

- **GIVEN** 没有已连接的 BLE 设备
- **WHEN** 浏览器发送键鼠事件
- **THEN** 应用丢弃事件且不报错，状态显示 BLE 未就绪
