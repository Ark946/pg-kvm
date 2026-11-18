/**
 * 服务器端口解析与 WebSocket 地址拼接。
 *
 * 端口来自设备 `GET /api/status` 的 `ports` 字段；页面与 HTTP 服务同源，
 * 因此主机名使用当前页面的 `window.location.hostname`。
 */

export interface ServerPorts {
  http: number;
  signaling: number;
  relay: number;
}

export interface ConnectionUrls {
  signalingUrl: string;
  inputUrl: string;
}

const MIN_PORT = 1;
const MAX_PORT = 65535;

function isValidPort(value: unknown): value is number {
  return (
    typeof value === "number" &&
    Number.isInteger(value) &&
    value >= MIN_PORT &&
    value <= MAX_PORT
  );
}

/**
 * 从 `/api/status` 响应中解析端口。
 *
 * 缺失、类型错误或超出 1-65535 时返回 `null`，调用方应阻止连接，
 * 不得回退到默认端口。
 */
export function parseServerPorts(status: unknown): ServerPorts | null {
  if (status === null || typeof status !== "object") return null;

  const ports = (status as { ports?: unknown }).ports;
  if (ports === null || typeof ports !== "object") return null;

  const { http, signaling, relay } = ports as Record<string, unknown>;
  if (!isValidPort(http) || !isValidPort(signaling) || !isValidPort(relay)) {
    return null;
  }

  return { http, signaling, relay };
}

/** 按页面主机名与端口拼接 WebSocket 地址。 */
export function buildWsUrl(hostname: string, port: number): string {
  return `ws://${hostname}:${port}`;
}

/**
 * 解析连接所需的信令与输入 WebSocket 地址。
 *
 * @returns 端口可用时返回两个地址；否则返回 `null`。
 */
export function resolveConnectionUrls(
  status: unknown,
  hostname: string
): ConnectionUrls | null {
  const ports = parseServerPorts(status);
  if (!ports) return null;

  return {
    signalingUrl: buildWsUrl(hostname, ports.signaling),
    inputUrl: buildWsUrl(hostname, ports.relay),
  };
}
