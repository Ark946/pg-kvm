import test from "node:test";
import assert from "node:assert/strict";
import { isDeviceStatus } from "../../../pg-kvm-android/frontend/src/device-status.ts";

const validStatus = {
  cameras: [{ id: "0", name: "Camera", current: true }],
  currentCameraName: "Camera",
  ports: { http: 8080, signaling: 3000, relay: 3001 },
  stream: {
    resolution: "1280x720",
    resolutionWidth: 1280,
    resolutionHeight: 720,
    frameRate: 30,
    codec: "VP8",
    isStreaming: true,
    serverUrl: "",
    clientAddresses: [],
  },
  ble: {
    status: "Ready",
    isReady: true,
    isScanning: false,
    connectedDeviceName: null,
    inputClientCount: 0,
  },
};

test("DST-01 合法状态返回 true", () => {
  assert.equal(isDeviceStatus(validStatus), true);
});

test("DST-02 缺少 ports 的旧版状态仍可用于展示", () => {
  const { ports: _ports, ...withoutPorts } = validStatus;
  assert.equal(isDeviceStatus(withoutPorts), true);
});

test("DST-03 会话未就绪错误体返回 false", () => {
  assert.equal(isDeviceStatus({ ok: false, message: "session not ready" }), false);
});

test("DST-04 空对象返回 false", () => {
  assert.equal(isDeviceStatus({}), false);
});

test("DST-05 缺少 stream 或 ble 返回 false", () => {
  assert.equal(isDeviceStatus({ cameras: [], streams: {} }), false);
  const { stream: _stream, ...withoutStream } = validStatus;
  assert.equal(isDeviceStatus(withoutStream), false);
});

test("DST-06 非对象返回 false", () => {
  assert.equal(isDeviceStatus(null), false);
  assert.equal(isDeviceStatus(undefined), false);
  assert.equal(isDeviceStatus("ok"), false);
  assert.equal(isDeviceStatus(42), false);
});
