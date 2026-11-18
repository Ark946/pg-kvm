package com.arkj.compose.stream.sessions

import android.content.Context
import android.util.Log
import android.view.Surface
import com.arkj.compose.models.ResolutionInfo
import com.arkj.compose.stream.input.BleConnection
import com.jiangdg.usb.USBMonitor
import com.arkj.video.capture.UvcCameraVideoCapture
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoSource

/**
 * 单个摄像头的会话对象（per-session）。
 *
 * 持有该摄像头专属的资源：
 * - [capturer]：UvcCameraVideoCapture（内含 UsbControlBlock / UVCCamera / 采集线程）
 * - [bleConnection]：与该摄像头配对的 ESP32 BLE 连接（仅活跃会话持有，切换时断开）
 *
 * 全局资源（USBMonitor、BluetoothAdapter 等）由 SessionManager 中的全局 Helper 持有，
 * 本类只做会话级生命周期管理。
 */
class CameraSession(
    val cameraId: String,
    val displayName: String
) {
    companion object {
        private const val TAG = "CameraSession"
        const val DEFAULT_WIDTH = 1280
        const val DEFAULT_HEIGHT = 720
        const val DEFAULT_FPS = 30
    }

    /** 当前采集器，未激活（dormant）时为 null */
    var capturer: UvcCameraVideoCapture? = null
        private set

    /** 当前配对的 BLE 连接，未连接时为 null */
    var bleConnection: BleConnection? = null
        private set

    /**
     * 当前会话生效的采集规格（默认 1280x720@30）。
     * 暂停/恢复预览、停止/重新开始串流时沿用该值。
     */
    var captureSpec: CaptureSpec = CaptureSpec.DEFAULT
        private set

    /** 最近一次设置的预览 Surface（切换/重启采集时复用） */
    var pendingPreviewSurface: Surface? = null

    /** 采集启动回调（分辨率、帧率），由 SessionManager 设置 */
    var onCaptureStarted: ((width: Int, height: Int, fps: Int) -> Unit)? = null

    /** BLE 意外断开的自动重连剩余尝试次数（连接成功后重置） */
    internal var bleReconnectAttempts: Int = 0

    // ==================== 采集生命周期 ====================

    /**
     * 创建 capturer 并以给定的 ctrlBlock 启动采集。
     * 在 SessionManager 拿到 USB 权限（onGranted）后调用。
     */
    @Synchronized
    fun startCapturer(
        context: Context,
        ctrlBlock: USBMonitor.UsbControlBlock,
        surfaceTextureHelper: SurfaceTextureHelper,
        videoSource: VideoSource
    ) {
        releaseCapturer()

        val capturer = UvcCameraVideoCapture()
        capturer.initialize(surfaceTextureHelper, context, videoSource.capturerObserver)
        capturer.setUsbControlBlock(ctrlBlock)
        pendingPreviewSurface?.let { capturer.setPreviewSurface(it) }
        this.capturer = capturer

        val spec = captureSpec
        capturer.startCapture(spec.width, spec.height, spec.fps)
        onCaptureStarted?.invoke(spec.width, spec.height, spec.fps)
    }

    /** 停止采集循环，保留 UVCCamera 对象（可 resume 恢复） */
    @Synchronized
    fun pauseCapture() {
        try {
            capturer?.stopCapture()
        } catch (e: InterruptedException) {
            Log.e(TAG, "stopCapture interrupted", e)
            Thread.currentThread().interrupt()
        }
    }

    /**
     * 恢复采集循环（pauseCapture 之后），沿用 [captureSpec]。
     *
     * @return 是否真正恢复了采集；没有 capturer 时返回 false。
     */
    @Synchronized
    fun resumeCapture(): Boolean {
        val capturer = this.capturer ?: return false
        val spec = captureSpec
        capturer.startCapture(spec.width, spec.height, spec.fps)
        onCaptureStarted?.invoke(spec.width, spec.height, spec.fps)
        return true
    }

    // ==================== 分辨率 ====================

    /**
     * 当前摄像头支持的分辨率列表（需摄像头已打开，否则返回空列表）。
     * 去重并按像素数降序排列。
     */
    @Synchronized
    fun supportedResolutions(): List<ResolutionInfo> {
        val capturer = this.capturer ?: return emptyList()
        return capturer.supportedResolutions
            .mapNotNull { size ->
                // fps 数组可能为空，取最大帧率，回退默认值
                val fps = size.fps?.maxOrNull()?.toInt()?.takeIf { it > 0 } ?: DEFAULT_FPS
              ResolutionInfo(size.width, size.height, fps)
            }
            .distinctBy { it.key }
            .sortedWith(compareByDescending<ResolutionInfo> { it.width * it.height }.thenByDescending { it.width })
    }

    /**
     * 切换采集分辨率：停止预览循环后以新尺寸重启（UVCCamera 保持打开，不重新申请权限）。
     *
     * @return 是否真正执行了切换；没有 capturer 时返回 false。
     */
    @Synchronized
    fun changeResolution(width: Int, height: Int, fps: Int): Boolean {
        val capturer = this.capturer ?: return false
        captureSpec = CaptureSpec(width, height, fps)
        pauseCapture()
        capturer.startCapture(width, height, fps)
        onCaptureStarted?.invoke(width, height, fps)
        return true
    }

    /** 恢复默认规格；下一次激活摄像头时使用。 */
    fun resetCaptureSpec() {
        captureSpec = CaptureSpec.DEFAULT
    }

    /** 释放采集器（完全释放原生资源），会话进入 dormant 状态 */
    @Synchronized
    fun releaseCapturer() {
        val capturer = this.capturer ?: return
        try {
            capturer.stopCapture()
        } catch (e: InterruptedException) {
            Log.e(TAG, "stopCapture interrupted", e)
            Thread.currentThread().interrupt()
        }
        capturer.dispose()
        this.capturer = null
    }

    /** 设置/更新本地预览 Surface（传 null 移除预览） */
    fun setPreviewSurface(surface: Surface?) {
        pendingPreviewSurface = surface
        capturer?.setPreviewSurface(surface)
    }

    // ==================== BLE 配对生命周期 ====================

    /**
     * 绑定并连接指定的 BLE 设备（替换已有连接）。
     * [connection] 应由全局 BleHelper 创建。
     */
    fun attachBle(connection: BleConnection) {
        releaseBle()
        bleConnection = connection
        connection.connect()
    }

    /** 断开当前 BLE 连接（保留配对记录，配对记录由 BlePairingStore 管理） */
    fun releaseBle() {
        bleConnection?.disconnect()
        bleConnection = null
    }

    // ==================== 总生命周期 ====================

    /** 释放本会话全部资源（采集器 + BLE 连接） */
    fun release() {
        releaseCapturer()
        releaseBle()
        pendingPreviewSurface = null
        onCaptureStarted = null
    }
}
