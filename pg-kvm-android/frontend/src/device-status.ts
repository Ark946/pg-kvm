import type { ServerPorts } from "./ports";

/** `/api/status` 返回的摄像头条目。 */
export interface CameraItem {
  id: string;
  name: string;
  current: boolean;
}

/** `/api/status` 的成功响应结构。 */
export interface DeviceStatus {
  cameras: CameraItem[];
  currentCameraName: string | null;
  /** 旧版设备可能不返回；连接前会再次做运行时校验。 */
  ports?: ServerPorts;
  stream: {
    resolution: string;
    resolutionWidth: number;
    resolutionHeight: number;
    frameRate: number;
    codec: string;
    isStreaming: boolean;
    serverUrl: string;
    clientAddresses: string[];
  };
  ble: {
    status: string;
    isReady: boolean;
    isScanning: boolean;
    connectedDeviceName: string | null;
    inputClientCount: number;
  };
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return value !== null && typeof value === "object";
}

/**
 * 判断 `/api/status` 响应是否是可用于展示的状态。
 *
 * 会话未就绪时接口返回 `{ok:false,message}`；若把这类响应写入界面状态，
 * 模板访问 `stream`/`ble` 会抛错并导致页面不可操作。因此只接受同时
 * 包含 `cameras`、`stream`、`ble` 的对象。
 */
export function isDeviceStatus(value: unknown): value is DeviceStatus {
  if (!isRecord(value)) return false;
  return (
    Array.isArray(value.cameras) &&
    isRecord(value.stream) &&
    isRecord(value.ble)
  );
}
