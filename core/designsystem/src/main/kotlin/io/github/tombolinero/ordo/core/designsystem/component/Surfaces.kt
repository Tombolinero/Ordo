package io.github.tombolinero.ordo.core.designsystem.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.animateFloatAsState
import io.github.tombolinero.ordo.core.designsystem.motion.OrdoMotion
import io.github.tombolinero.ordo.core.designsystem.shape.OrdoShapes
import io.github.tombolinero.ordo.core.designsystem.theme.OrdoPalette
import kotlin.math.hypot
import kotlin.math.max

/** Surface finishes of the cards. Every list picks one as its card colour. */
@Immutable
enum class PillowStyle { Graphite, Magenta, Violet, Ruby, Cobalt, Petrol }

/** Text on cards is always white; the secondary line is slightly dimmed. */
fun PillowStyle.contentColor(): Color = Color.White

fun PillowStyle.secondaryContentColor(): Color = Color.White.copy(alpha = 0.66f)

private fun Modifier.pillowFill(style: PillowStyle): Modifier = drawWithCache {
    val w = size.width
    val h = size.height
    val diag = hypot(w, h)
    val brush = when (style) {
        PillowStyle.Graphite -> Brush.radialGradient(
            0f to OrdoPalette.Graphite700,
            0.48f to OrdoPalette.Graphite800,
            1f to OrdoPalette.Graphite850,
            center = Offset(w * 0.28f, h * 0.18f),
            radius = diag * 0.95f,
        )
        PillowStyle.Magenta -> Brush.radialGradient(
            0f to OrdoPalette.PurpleGlow,
            0.18f to OrdoPalette.PurpleLight,
            0.55f to OrdoPalette.Purple,
            1f to OrdoPalette.PurpleDeep,
            center = Offset(w * 0.14f, h * 0.08f),
            radius = diag * 1.05f,
        )
        PillowStyle.Violet -> Brush.radialGradient(
            0.35f to OrdoPalette.VioletCore,
            0.70f to OrdoPalette.VioletMuted,
            0.92f to OrdoPalette.VioletMid,
            1f to OrdoPalette.Violet,
            center = Offset(w * 0.5f, h * 0.45f),
            radius = max(w, h) * 0.78f,
        )
        PillowStyle.Ruby -> Brush.radialGradient(
            0f to Color(0xFFFFA3B0),
            0.18f to Color(0xFFE2475A),
            0.58f to Color(0xFFB3203F),
            1f to Color(0xFF5E0C24),
            center = Offset(w * 0.14f, h * 0.08f),
            radius = diag * 1.05f,
        )
        PillowStyle.Cobalt -> Brush.radialGradient(
            0f to Color(0xFF9DBBFF),
            0.18f to Color(0xFF4A7CF0),
            0.56f to Color(0xFF2346C2),
            1f to Color(0xFF0E1F63),
            center = Offset(w * 0.14f, h * 0.08f),
            radius = diag * 1.05f,
        )
        PillowStyle.Petrol -> Brush.radialGradient(
            0.35f to Color(0xFF01161A),
            0.70f to Color(0xFF07393D),
            0.92f to Color(0xFF0E7C74),
            1f to Color(0xFF19B3A3),
            center = Offset(w * 0.5f, h * 0.45f),
            radius = max(w, h) * 0.78f,
        )
    }
    onDrawBehind { drawRect(brush) }
}

/**
 * The soft, inflated card of the reference design: gradient fill lit from the top left,
 * a faint light rim on the upper edges and a deep, blurred shadow.
 */
@Composable
fun PillowSurface(
    modifier: Modifier = Modifier,
    style: PillowStyle = PillowStyle.Graphite,
    shape: Shape = OrdoShapes.Card,
    elevation: Dp = 14.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier
            .dropShadow(shape, Shadow(radius = elevation * 2, color = Color.Black.copy(alpha = 0.7f), offset = DpOffset(0.dp, elevation * 0.7f)))
            .clip(shape)
            .pillowFill(style)
            .innerShadow(shape, Shadow(radius = 2.dp, color = Color.White.copy(alpha = if (style == PillowStyle.Graphite) 0.07f else 0.16f), offset = DpOffset(1.dp, 1.dp)))
            .innerShadow(shape, Shadow(radius = 18.dp, color = Color.Black.copy(alpha = 0.22f), offset = DpOffset((-4).dp, (-6).dp))),
        content = content,
    )
}

/** Faint dot grid behind every screen, as in the reference. */
fun Modifier.dotGridBackground(
    background: Color = Color.Black,
    dot: Color = Color.White.copy(alpha = 0.055f),
    spacing: Dp = 16.dp,
): Modifier = drawWithCache {
    val step = spacing.toPx()
    val points = ArrayList<Offset>()
    var y = step / 2
    while (y < size.height) {
        var x = step / 2
        while (x < size.width) {
            points += Offset(x, y)
            x += step
        }
        y += step
    }
    val stroke = 1.6.dp.toPx()
    onDrawBehind {
        drawRect(background)
        drawPoints(points, PointMode.Points, dot, strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

/** Scales the element down while pressed, springing back on release. */
@Composable
fun Modifier.pressScale(interactionSource: MutableInteractionSource, pressedScale: Float = OrdoMotion.PressedScale): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) pressedScale else 1f, OrdoMotion.snappy(), label = "press")
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

