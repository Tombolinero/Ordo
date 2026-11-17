package io.github.tombolinero.ordo.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import io.github.tombolinero.ordo.core.designsystem.motion.OrdoMotion
import io.github.tombolinero.ordo.core.designsystem.shape.OrdoShapes
import io.github.tombolinero.ordo.core.designsystem.theme.OrdoPalette
import io.github.tombolinero.ordo.core.designsystem.theme.OrdoTheme
import kotlin.math.hypot

/**
 * Round "glass" button of the reference: dark centre, softly lit rim, deep shadow.
 */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    iconSize: Dp = 20.dp,
    tint: Color = Color.White,
    enabled: Boolean = true,
    highlighted: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val haptics = LocalHapticFeedback.current
    Box(
        modifier
            .size(size)
            .pressScale(interaction, 0.92f)
            .dropShadow(CircleShape, Shadow(radius = 14.dp, color = Color.Black.copy(alpha = 0.75f), offset = DpOffset(0.dp, 6.dp)))
            .clip(CircleShape)
            .glassFill(highlighted)
            .clickable(interaction, indication = null, enabled = enabled, role = Role.Button) {
                haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription,
            Modifier.size(iconSize),
            tint = when {
                highlighted -> OrdoPalette.YellowInk
                enabled -> tint
                else -> tint.copy(alpha = 0.3f)
            },
        )
    }
}

private fun Modifier.glassFill(highlighted: Boolean) = drawWithCache {
    val brush = if (highlighted) {
        Brush.radialGradient(
            0f to OrdoPalette.YellowLight, 0.6f to OrdoPalette.Yellow, 1f to OrdoPalette.YellowDeep,
            center = Offset(size.width * 0.4f, size.height * 0.3f), radius = size.maxDimension * 0.8f,
        )
    } else if (size.width > size.height * 1.3f) {
        // Wide pills: light falls from the top instead of a centred glow.
        Brush.verticalGradient(0f to Color(0xFF2E2E2E), 0.5f to Color(0xFF1A1A1A), 1f to Color(0xFF101010))
    } else {
        Brush.radialGradient(
            0.55f to Color(0xFF080808), 0.85f to Color(0xFF1C1C1C), 1f to Color(0xFF2E2E2E),
            center = Offset(size.width / 2f, size.height / 2f), radius = size.minDimension / 2f,
        )
    }
    onDrawBehind { drawRect(brush) }
}

/**
 * The yellow primary pill with its speaker-grille dot texture. Used once per screen for
 * the main action ("new list", "new entry").
 */
@Composable
fun DotPillButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 104.dp,
    height: Dp = 56.dp,
    icon: ImageVector? = null,
    contentDescription: String? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val haptics = LocalHapticFeedback.current
    Box(
        modifier
            .size(width, height)
            .pressScale(interaction, 0.94f)
            .dropShadow(OrdoShapes.Pill, Shadow(radius = 22.dp, color = Color.Black.copy(alpha = 0.7f), offset = DpOffset(0.dp, 10.dp)))
            .dropShadow(OrdoShapes.Pill, Shadow(radius = 26.dp, color = OrdoPalette.Yellow.copy(alpha = 0.16f)))
            .clip(OrdoShapes.Pill)
            .dotPillFill()
            .innerShadow(OrdoShapes.Pill, Shadow(radius = 5.dp, color = Color.White.copy(alpha = 0.45f), offset = DpOffset(0.dp, 2.dp)))
            .innerShadow(OrdoShapes.Pill, Shadow(radius = 10.dp, color = Color(0x553A2F00), offset = DpOffset(0.dp, (-4).dp)))
            .clickable(interaction, indication = null, role = Role.Button) {
                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) {
            Box(Modifier.size(34.dp).clip(CircleShape).dotPillCenter(), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription, Modifier.size(20.dp), tint = OrdoPalette.YellowInk)
            }
        }
    }
}

private fun Modifier.dotPillFill() = drawWithCache {
    val base = Brush.radialGradient(
        0f to OrdoPalette.YellowLight, 0.45f to OrdoPalette.Yellow, 1f to OrdoPalette.YellowDeep,
        center = Offset(size.width * 0.4f, 0f), radius = hypot(size.width, size.height) * 0.85f,
    )
    val spacing = 7.dp.toPx()
    val dotR = 1.25.dp.toPx()
    val pts = ArrayList<Pair<Offset, Float>>()
    val cx = size.width / 2
    val cy = size.height / 2
    var y = spacing * 0.75f
    while (y < size.height) {
        var x = spacing * 0.75f
        while (x < size.width) {
            // Fade the grille towards the rim, like a perforated speaker cover.
            val nx = (x - cx) / (size.width / 2)
            val ny = (y - cy) / (size.height / 2)
            val d = hypot(nx, ny * 0.9f)
            val a = (1f - d * d).coerceIn(0f, 1f)
            if (a > 0.05f) pts += Offset(x, y) to a
            x += spacing
        }
        y += spacing
    }
    onDrawBehind {
        drawRect(base)
        pts.forEach { (o, a) -> drawCircle(OrdoPalette.YellowInk.copy(alpha = 0.34f * a), dotR, o) }
    }
}

private fun Modifier.dotPillCenter() = drawWithCache {
    val b = Brush.radialGradient(0f to OrdoPalette.YellowLight, 1f to OrdoPalette.Yellow, center = Offset(size.width * 0.4f, size.height * 0.3f))
    onDrawBehind { drawRect(b) }
}

/** Secondary action: a quiet text pill on a dark glass surface. */
@Composable
fun GlassPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    accent: Boolean = false,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val haptics = LocalHapticFeedback.current
    val fg = when {
        !enabled -> Color.White.copy(alpha = 0.3f)
        accent -> OrdoPalette.YellowInk
        else -> Color.White
    }
    Row(
        modifier
            .height(48.dp)
            .pressScale(interaction)
            .dropShadow(OrdoShapes.Pill, Shadow(radius = 14.dp, color = Color.Black.copy(alpha = 0.6f), offset = DpOffset(0.dp, 6.dp)))
            .clip(OrdoShapes.Pill)
            .then(if (accent) Modifier.dotPillCenter() else Modifier.glassFill(false))
            .clickable(interaction, indication = null, enabled = enabled, role = Role.Button) {
                haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                onClick()
            }
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        if (icon != null) Icon(icon, null, Modifier.size(18.dp), tint = fg)
        Text(text, style = OrdoTheme.type.button, color = fg, maxLines = 1, softWrap = false)
    }
}

/** Toggle of the reference ("Sync"): dark track, white knob that springs across. */
@Composable
fun OrdoToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val knobX by animateDpAsState(if (checked) 24.dp else 3.dp, OrdoMotion.bouncy(Dp.VisibilityThreshold), label = "knob")
    val track by animateColorAsState(if (checked) OrdoPalette.Yellow else OrdoPalette.ToggleTrack, OrdoMotion.smooth(), label = "track")
    Box(
        modifier
            .size(width = 56.dp, height = 34.dp)
            .clip(OrdoShapes.Pill)
            .drawWithCache { onDrawBehind { drawRect(track) } }
            .innerShadow(OrdoShapes.Pill, Shadow(radius = 4.dp, color = Color.Black.copy(alpha = 0.45f), offset = DpOffset(0.dp, 2.dp)))
            .toggleable(checked, role = Role.Switch) {
                haptics.performHapticFeedback(if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
                onCheckedChange(it)
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .offset(x = knobX)
                .size(28.dp)
                .dropShadow(CircleShape, Shadow(radius = 6.dp, color = Color.Black.copy(alpha = 0.4f), offset = DpOffset(0.dp, 2.dp)))
                .clip(CircleShape)
                .drawWithCache {
                    val b = Brush.radialGradient(0f to Color.White, 1f to Color(0xFFE4E4E4), center = Offset(size.width * 0.35f, size.height * 0.3f))
                    onDrawBehind { drawRect(b) }
                },
        )
    }
}

/** Bottom navigation of the reference: two glass buttons around the yellow dot pill. */
@Composable
fun FloatingNavBar(
    modifier: Modifier = Modifier,
    leading: @Composable RowScope.() -> Unit,
    primary: @Composable RowScope.() -> Unit,
    trailing: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        primary()
        trailing()
    }
}

