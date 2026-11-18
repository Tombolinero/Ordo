package io.github.tombolinero.ordo.core.ui

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** Regression tests for the crash while adjusting a picture in the crop screen (1.0.0). */
class CropMathTest {

    @Test fun `picture exactly filling the frame does not crash (float rounding)`() {
        // 1600 px * (998.4 / 1600) is a hair below 998.4 in Float: the old code built the
        // range 0.00003..-0.00003 and crashed.
        val frame = 998.4f
        val scale = frame / 1600f
        val image = 1600f * scale
        assertThat(image - frame).isAtMost(0f)
        assertThat(CropMath.clampOffset(12f, image, frame)).isEqualTo(0f)
        assertThat(CropMath.clampOffset(-3f, image, frame)).isEqualTo(0f)
    }

    @Test fun `many real sizes never throw`() {
        for (w in listOf(480, 1080, 1199, 1440, 1600, 3000, 4032)) {
            for (h in listOf(360, 1080, 1201, 1600, 2268, 3024)) {
                for (frame in listOf(301.7f, 906f, 998.4f, 1003.2f)) {
                    val min = maxOf(frame / w, frame / h)
                    for (s in listOf(min, min * 0.999999f, min * 1.000001f, min * 2.37f)) {
                        CropMath.clampOffset(17f, w * s, frame)
                        CropMath.clampOffset(-9999f, h * s, frame)
                        val rect = CropMath.cropRect(5f, -5f, w * s, h * s, frame)
                        assertThat(rect.right).isGreaterThan(rect.left)
                        assertThat(rect.bottom).isGreaterThan(rect.top)
                    }
                }
            }
        }
    }

    @Test fun `offsets are limited so the picture always covers the frame`() {
        assertThat(CropMath.clampOffset(500f, 1200f, 1000f)).isEqualTo(100f)
        assertThat(CropMath.clampOffset(-500f, 1200f, 1000f)).isEqualTo(-100f)
        assertThat(CropMath.clampOffset(40f, 1200f, 1000f)).isEqualTo(40f)
    }

    @Test fun `broken numbers from the gesture system are ignored`() {
        assertThat(CropMath.clampOffset(Float.NaN, 1200f, 1000f)).isEqualTo(0f)
        assertThat(CropMath.clampOffset(10f, Float.NaN, 1000f)).isEqualTo(0f)
        assertThat(CropMath.cropRect(Float.NaN, 0f, Float.POSITIVE_INFINITY, 1000f, 1000f)).isNotNull()
    }

    @Test fun `centred square picture keeps its middle`() {
        val r = CropMath.cropRect(0f, 0f, 2000f, 1000f, 1000f)
        assertThat(r.left).isWithin(0.001f).of(0.25f)
        assertThat(r.right).isWithin(0.001f).of(0.75f)
        assertThat(r.top).isWithin(0.001f).of(0f)
        assertThat(r.bottom).isWithin(0.001f).of(1f)
    }
}
