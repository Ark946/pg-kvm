# webrtc-streaming Specification

## Purpose

把应用内的视频轨通过 WebRTC 发布给同一网络中的浏览器，并为每个浏览器维护独立、可清理的信令与媒体连接。

## Requirements

### Requirement: 信令端点

应用 SHALL 在配置的信令端口上提供 WebSocket 信令端点。非法 JSON、缺少必需字段或未知消息类型 MUST NOT 终止服务器运行，也 MUST NOT 影响其他客户端连接。

#### Scenario: 客户端连接

- **WHEN** 浏览器连接到信令端口并发送合法 offer
- **THEN** 应用受理该连接并进入 offer/answer 协商

#### Scenario: 非法消息

- **WHEN** 客户端发送非法 JSON、缺少 `sdp` 的 offer 或未知 `type`
- **THEN** 应用忽略该消息，服务器保持运行，其他客户端不受影响

### Requirement: 每客户端独立连接

每个浏览器连接 SHALL 对应一个独立的 WebRTC 连接；任一连接关闭 MUST NOT 影响其他连接。应用 SHALL 向设备状态暴露当前客户端地址列表和客户端数量。

#### Scenario: 多个浏览器同时观看

- **WHEN** 两个浏览器分别建立连接
- **THEN** 它们各自收到视频，状态中的客户端地址列表包含两项

#### Scenario: 单个客户端断开

- **GIVEN** 两个浏览器正在观看
- **WHEN** 其中一个断开
- **THEN** 其 WebRTC 连接被释放，另一个继续正常接收视频

### Requirement: Offer/Answer 协商

浏览器 SHALL 作为 offer 发起方；应用 SHALL 以 answer 响应，并在 answer 中挂载当前视频轨。缺少 SDP 的 offer MUST 被忽略；没有可用视频轨时，应用 SHALL 返回不含视频的 answer。

#### Scenario: 正常协商

- **WHEN** 应用收到包含有效 SDP 的 offer 且视频轨可用
- **THEN** 应用返回 answer，其中包含本地视频轨

#### Scenario: 缺少 SDP

- **WHEN** 应用收到不带 `sdp` 字段的 offer
- **THEN** 应用忽略该消息，不发送 answer

#### Scenario: 视频轨不可用

- **GIVEN** 当前没有可用的本地视频轨
- **WHEN** 应用收到合法 offer
- **THEN** 应用仍返回 answer，但不包含视频轨

### Requirement: ICE 候选交换

应用 SHALL 在信令通道上双向转发 ICE candidate。空 candidate 表示收集结束，MUST 被忽略；在远端描述设置前到达的 candidate SHALL 被缓存，并在远端描述就绪后添加。

#### Scenario: 双向转发

- **WHEN** 浏览器或应用产生非空 ICE candidate
- **THEN** 该 candidate 被转发给对端

#### Scenario: 提前到达的 candidate

- **GIVEN** 应用尚未设置远端描述
- **WHEN** 收到浏览器的 ICE candidate
- **THEN** 应用缓存该 candidate，并在设置远端描述后添加

#### Scenario: 收集结束

- **WHEN** 任一端发送空的 candidate
- **THEN** 应用忽略该消息，不转发也不报错

### Requirement: 视频轨连续性

切换活跃摄像头 SHALL NOT 重建对外视频轨，也 MUST NOT 要求浏览器重新协商；已连接的浏览器 SHALL 继续接收新摄像头的画面。

#### Scenario: 串流中切换摄像头

- **GIVEN** 浏览器正在接收摄像头 A 的画面
- **WHEN** 用户切换到摄像头 B
- **THEN** 浏览器无需重连或重新协商，画面切换为摄像头 B

### Requirement: 仅发布视频

v1 的 WebRTC 流 SHALL 只包含视频轨，MUST NOT 发布音频轨。

#### Scenario: 浏览器接收媒体

- **WHEN** 浏览器建立连接并收到远端媒体
- **THEN** 只存在视频轨，不存在音频轨

### Requirement: 连接清理

浏览器断开或信令服务器停止时，应用 SHALL 关闭对应的 PeerConnection 并释放其资源。清理单个连接 MUST NOT 停止服务器接受新连接。

#### Scenario: 客户端关闭页面

- **WHEN** 浏览器关闭页面导致 WebSocket 断开
- **THEN** 应用关闭该 PeerConnection、更新客户端列表，并继续接受新连接

#### Scenario: 停止信令服务

- **WHEN** 用户停止串流服务
- **THEN** 所有客户端连接被关闭，端口被释放
