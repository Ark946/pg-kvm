import test from "node:test";
import assert from "node:assert/strict";
import { resolveConnectionUrls } from "../../../pg-kvm-android/frontend/src/ports.ts";

const HOST = "192.168.1.10";

test("UTC-01 默认端口生成 3000/3001 的 WS 地址", () => {
  const urls = resolveConnectionUrls(
    { ports: { http: 8080, signaling: 3000, relay: 3001 } },
    HOST
  );
  assert.deepEqual(urls, {
    signalingUrl: "ws://192.168.1.10:3000",
    inputUrl: "ws://192.168.1.10:3001",
  });
});

test("UTC-02 自定义端口生成 4000/4001 的 WS 地址", () => {
  const urls = resolveConnectionUrls(
    { ports: { http: 8080, signaling: 4000, relay: 4001 } },
    HOST
  );
  assert.deepEqual(urls, {
    signalingUrl: "ws://192.168.1.10:4000",
    inputUrl: "ws://192.168.1.10:4001",
  });
});

test("UTC-03 使用传入的页面主机名", () => {
  const urls = resolveConnectionUrls(
    { ports: { http: 8080, signaling: 3000, relay: 3001 } },
    "kvm.local"
  );
  assert.deepEqual(urls, {
    signalingUrl: "ws://kvm.local:3000",
    inputUrl: "ws://kvm.local:3001",
  });
});

test("UTC-04 缺少 ports 字段时返回 null", () => {
  assert.equal(resolveConnectionUrls({}, HOST), null);
  assert.equal(resolveConnectionUrls(null, HOST), null);
  assert.equal(resolveConnectionUrls({ ports: null }, HOST), null);
  assert.equal(resolveConnectionUrls({ ports: "3000" }, HOST), null);
});

test("UTC-05 缺少任一端口时返回 null", () => {
  assert.equal(resolveConnectionUrls({ ports: { http: 8080, signaling: 3000 } }, HOST), null);
  assert.equal(resolveConnectionUrls({ ports: { http: 8080, relay: 3001 } }, HOST), null);
  assert.equal(resolveConnectionUrls({ ports: { signaling: 3000, relay: 3001 } }, HOST), null);
});

test("UTC-06 非法端口值返回 null", () => {
  const invalidValues = [0, 65536, -1, 3.5, "4000", null, undefined, NaN, Infinity];
  for (const invalid of invalidValues) {
    const status = {
      ports: { http: 8080, signaling: 3000, relay: invalid },
    };
    assert.equal(
      resolveConnectionUrls(status, HOST),
      null,
      `relay=${String(invalid)} 应视为不可用`
    );
  }
});

test("UTC-07 端口下界 1 与上界 65535 可用", () => {
  assert.deepEqual(resolveConnectionUrls({ ports: { http: 1, signaling: 1, relay: 1 } }, HOST), {
    signalingUrl: "ws://192.168.1.10:1",
    inputUrl: "ws://192.168.1.10:1",
  });
  assert.deepEqual(
    resolveConnectionUrls({ ports: { http: 65535, signaling: 65535, relay: 65535 } }, HOST),
    {
      signalingUrl: "ws://192.168.1.10:65535",
      inputUrl: "ws://192.168.1.10:65535",
    }
  );
});

test("UTC-08 状态不是对象时返回 null", () => {
  assert.equal(resolveConnectionUrls(null, HOST), null);
  assert.equal(resolveConnectionUrls(undefined, HOST), null);
  assert.equal(resolveConnectionUrls("ok", HOST), null);
  assert.equal(resolveConnectionUrls(42, HOST), null);
});
