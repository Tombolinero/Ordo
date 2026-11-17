package io.github.tombolinero.ordo.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.tombolinero.ordo.core.designsystem.motion.OrdoMotion
import io.github.tombolinero.ordo.core.designsystem.theme.OrdoPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The "Energy Status" gauge of the reference, reinterpreted as a rank wheel.
 *
 * The ring is cut into [count] equal segments, one per rank. Ticks up to and including
 * [position] are lit, every fourth lit tick is a long major tick, and a yellow marker
 * sits in the middle of the current segment. Changing [position] glides the marker
 * around the ring on a spring; on first composition the lit arc sweeps in.
 */
@Composable
fun RankWheel(
    count: Int,
    position: Int,
    modifier: Modifier = Modifier,
    diameter: Dp = 260.dp,
    ticks: Int = 120,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val safeCount = count.coerceAtLeast(1)
    val target = ((position.coerceIn(1, safeCount) - 0.5f) / safeCount).coerceIn(0f, 1f)
    val sweep = remember { Animatable(0f) }
    LaunchedEffect(target) { sweep.animateTo(target, OrdoMotion.fluid()) }
    val tick = OrdoPalette.White.copy(alpha = 0.2f)
    val lit = OrdoPalette.White.copy(alpha = 0.6f)
    val major = OrdoPalette.White.copy(alpha = 0.95f)
    Box(modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(diameter)) {
            val r = size.minDimension / 2f - 2.dp.toPx()
            val minor = 9.dp.toPx()
            val long = 20.dp.toPx()
            val markerLen = 26.dp.toPx()
            val progress = sweep.value
            for (i in 0 until ticks) {
                val f = i / ticks.toFloat()
                val angle = (f * 2 * PI - PI / 2).toFloat()
                val isLit = f <= progress
                val isMajor = isLit && i % 4 == 0
                val len = if (isMajor) long else minor
                val color = when {
                    isMajor -> major
                    isLit -> lit
                    else -> tick
                }
                val w = if (isMajor) 2.dp.toPx() else 1.3.dp.toPx()
                drawLine(color, polar(angle, r), polar(angle, r - len), w, StrokeCap.Round)
            }
            val ma = (progress * 2 * PI - PI / 2).toFloat()
            drawLine(OrdoPalette.Yellow, polar(ma, r + 1.dp.toPx()), polar(ma, r - markerLen), 3.dp.toPx(), StrokeCap.Round)
        }
        content()
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.polar(angle: Float, radius: Float) =
    Offset(center.x + cos(angle) * radius, center.y + sin(angle) * radius)

/**
 * The "Strain" ring of the reference: a dotted track with a yellow progress arc that starts
 * at twelve o'clock.
 */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    diameter: Dp = 84.dp,
    track: Color = Color.White.copy(alpha = 0.38f),
    arc: Color = OrdoPalette.Yellow,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), OrdoMotion.fluid(), label = "ring")
    Box(modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(diameter)) {
            val stroke = 3.dp.toPx()
            val inset = stroke
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            val dot = 1.4.dp.toPx()
            drawArc(
                track, 0f, 360f, false, Offset(inset, inset), arcSize,
                style = Stroke(dot, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(0.01f, 4.dp.toPx()))),
            )
            drawArc(arc, -90f, 360f * p, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        content()
    }
}
