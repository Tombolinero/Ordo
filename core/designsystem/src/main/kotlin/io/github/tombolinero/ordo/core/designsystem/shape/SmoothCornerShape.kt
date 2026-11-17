package io.github.tombolinero.ordo.core.designsystem.shape

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Rounded rectangle with continuous ("squircle") corners, the way iOS and Figma draw them.
 * A plain circular corner has a visible kink where the arc meets the straight edge;
 * smoothing spreads the curve over a longer distance so the surface looks soft and pillowy.
 *
 * Based on Figma's corner smoothing (MartinRGB / figma-squircle).
 *
 * @param smoothing 0 = plain circular corner, 0.6 = iOS-like.
 */
@Immutable
class SmoothCornerShape(
    private val radius: Dp,
    private val smoothing: Float = 0.6f,
) : Shape {

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val r = with(density) { radius.toPx() }
        if (r <= 0f || size.minDimension <= 0f) {
            return Outline.Rectangle(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height))
        }
        val path = PathParser().parsePathString(svgPath(size.width, size.height, r, smoothing)).toPath()
        return Outline.Generic(path)
    }

    override fun equals(other: Any?): Boolean =
        other is SmoothCornerShape && other.radius == radius && other.smoothing == smoothing

    override fun hashCode(): Int = 31 * radius.hashCode() + smoothing.hashCode()

    companion object {
        internal fun svgPath(width: Float, height: Float, cornerRadius: Float, cornerSmoothing: Float): String {
            val maxRadius = min(width, height) / 2f
            val r = min(cornerRadius, maxRadius)
            var s = cornerSmoothing
            var p = (1 + s) * r
            if (p > maxRadius) {
                s = (maxRadius / r - 1).coerceIn(0f, s)
                p = min(p, maxRadius)
            }
            val arcMeasure = 90f * (1 - s)
            val arcSection = sin(rad(arcMeasure / 2)) * r * sqrt(2f)
            val angleAlpha = (90f - arcMeasure) / 2f
            val p3ToP4 = r * tan(rad(angleAlpha / 2))
            val angleBeta = 45f * s
            val c = p3ToP4 * cos(rad(angleBeta))
            val d = c * tan(rad(angleBeta))
            val b = (p - arcSection - c - d) / 3f
            val a = 2f * b
            val abc = a + b + c
            val bc = b + c
            fun f(v: Float) = "%.3f".format(java.util.Locale.ROOT, v)
            return buildString {
                append("M ${f(width - p)} 0 ")
                append("c ${f(a)} 0 ${f(a + b)} 0 ${f(abc)} ${f(d)} ")
                append("a ${f(r)} ${f(r)} 0 0 1 ${f(arcSection)} ${f(arcSection)} ")
                append("c ${f(d)} ${f(c)} ${f(d)} ${f(bc)} ${f(d)} ${f(abc)} ")
                append("L ${f(width)} ${f(height - p)} ")
                append("c 0 ${f(a)} 0 ${f(a + b)} ${f(-d)} ${f(abc)} ")
                append("a ${f(r)} ${f(r)} 0 0 1 ${f(-arcSection)} ${f(arcSection)} ")
                append("c ${f(-c)} ${f(d)} ${f(-bc)} ${f(d)} ${f(-abc)} ${f(d)} ")
                append("L ${f(p)} ${f(height)} ")
                append("c ${f(-a)} 0 ${f(-(a + b))} 0 ${f(-abc)} ${f(-d)} ")
                append("a ${f(r)} ${f(r)} 0 0 1 ${f(-arcSection)} ${f(-arcSection)} ")
                append("c ${f(-d)} ${f(-c)} ${f(-d)} ${f(-bc)} ${f(-d)} ${f(-abc)} ")
                append("L 0 ${f(p)} ")
                append("c 0 ${f(-a)} 0 ${f(-(a + b))} ${f(d)} ${f(-abc)} ")
                append("a ${f(r)} ${f(r)} 0 0 1 ${f(arcSection)} ${f(-arcSection)} ")
                append("c ${f(c)} ${f(-d)} ${f(bc)} ${f(-d)} ${f(abc)} ${f(-d)} ")
                append("Z")
            }
        }

        private fun rad(deg: Float) = deg * (Math.PI.toFloat() / 180f)
    }
}

/** Shape tokens. Large radii on purpose: the reference design reads as soft, inflated surfaces. */
object OrdoShapes {
    val Card = SmoothCornerShape(34.dp)
    val CardSmall = SmoothCornerShape(26.dp)
    val Row = SmoothCornerShape(24.dp)
    val Thumb = SmoothCornerShape(16.dp)
    val ThumbLarge = SmoothCornerShape(24.dp)
    val Sheet = SmoothCornerShape(36.dp)
    val Field = SmoothCornerShape(20.dp)
    val Pill = androidx.compose.foundation.shape.RoundedCornerShape(percent = 50)
}
