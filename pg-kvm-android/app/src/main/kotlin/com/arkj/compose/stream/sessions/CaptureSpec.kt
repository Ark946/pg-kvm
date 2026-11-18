package com.arkj.compose.stream.sessions

import com.arkj.compose.models.ResolutionInfo

/** 一次采集请求的完整规格（宽、高、帧率）。 */
data class CaptureSpec(
  val width: Int,
  val height: Int,
  val fps: Int
) {
  companion object {
    /** 默认规格：1280x720@30。 */
    val DEFAULT = CaptureSpec(
      CameraSession.DEFAULT_WIDTH,
      CameraSession.DEFAULT_HEIGHT,
      CameraSession.DEFAULT_FPS
    )
  }

  val resolution: String get() = "${width}x$height"
}

/**
 * 在摄像头支持列表中查找与首选规格宽高匹配的项。
 *
 * 支持列表对每个尺寸只暴露最大帧率，因此帧率不参与匹配；应用时仍沿用
 * 用户选择的帧率，实际采集帧率由摄像头自身的范围决定。
 */
internal fun findSupportedSpec(
  preferred: CaptureSpec,
  supported: List<ResolutionInfo>
): ResolutionInfo? = supported.firstOrNull {
  it.width == preferred.width && it.height == preferred.height
}
