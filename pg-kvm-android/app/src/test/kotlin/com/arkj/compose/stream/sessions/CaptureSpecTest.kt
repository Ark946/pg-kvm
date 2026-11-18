package com.arkj.compose.stream.sessions

import com.arkj.compose.models.ResolutionInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CaptureSpecTest {

  @Test
  fun `default spec is 1280x720 at 30 fps`() {
    assertEquals(CaptureSpec(1280, 720, 30), CaptureSpec.DEFAULT)
  }

  @Test
  fun `returns matching supported spec`() {
    val supported = listOf(
      ResolutionInfo(1280, 720, 30),
      ResolutionInfo(1920, 1080, 30)
    )

    assertEquals(
      ResolutionInfo(1920, 1080, 30),
      findSupportedSpec(CaptureSpec(1920, 1080, 30), supported)
    )
  }

  @Test
  fun `matches by size when fps differs`() {
    val supported = listOf(ResolutionInfo(1920, 1080, 60))

    assertEquals(
      ResolutionInfo(1920, 1080, 60),
      findSupportedSpec(CaptureSpec(1920, 1080, 30), supported)
    )
  }

  @Test
  fun `returns null when supported list is empty`() {
    assertNull(findSupportedSpec(CaptureSpec(1920, 1080, 30), emptyList()))
  }

  @Test
  fun `returns null when size is absent`() {
    val supported = listOf(
      ResolutionInfo(1280, 720, 30),
      ResolutionInfo(640, 480, 30)
    )

    assertNull(findSupportedSpec(CaptureSpec(1920, 1080, 30), supported))
  }
}
