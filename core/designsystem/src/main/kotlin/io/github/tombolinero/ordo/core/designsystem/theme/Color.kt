package io.github.tombolinero.ordo.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Raw palette, taken from the reference design (see docs/DESIGN.md).
 * Components never use these directly; they read [OrdoColors] via [OrdoTheme.colors].
 */
object OrdoPalette {
    val Black = Color(0xFF000000)
    val Navy = Color(0xFF000B1A)
    val White = Color(0xFFFFFFFF)
    val Yellow = Color(0xFFF6D818)
    val YellowLight = Color(0xFFFFF06A)
    val YellowDeep = Color(0xFFD9BC0C)
    val YellowInk = Color(0xFF3A2F00)
    val Magenta = Color(0xFFBB2C90)
    val Purple = Color(0xFF933193)
    val PurpleLight = Color(0xFFB671B4)
    val PurpleGlow = Color(0xFFD79BD0)
    val PurpleDeep = Color(0xFF642B7E)
    val Violet = Color(0xFF6730DC)
    val VioletMid = Color(0xFF4B37B0)
    val VioletMuted = Color(0xFF1A1D4A)
    val VioletCore = Color(0xFF03041A)
    val Graphite900 = Color(0xFF0B0B0C)
    val Graphite850 = Color(0xFF111111)
    val Graphite800 = Color(0xFF1B1B1B)
    val Graphite700 = Color(0xFF2C2C2C)
    val Graphite600 = Color(0xFF3A3A3A)
    val ToggleTrack = Color(0xFF262A3B)
    val Danger = Color(0xFFFF5A5F)
}

@Immutable
data class OrdoColors(
    val background: Color = OrdoPalette.Black,
    val dotGrid: Color = OrdoPalette.White.copy(alpha = 0.055f),
    val textPrimary: Color = OrdoPalette.White,
    val textSecondary: Color = OrdoPalette.White.copy(alpha = 0.58f),
    val textTertiary: Color = OrdoPalette.White.copy(alpha = 0.36f),
    val accent: Color = OrdoPalette.Yellow,
    val onAccent: Color = OrdoPalette.YellowInk,
    val tick: Color = OrdoPalette.White.copy(alpha = 0.22f),
    val tickActive: Color = OrdoPalette.White.copy(alpha = 0.62f),
    val tickMajor: Color = OrdoPalette.White.copy(alpha = 0.95f),
    val divider: Color = OrdoPalette.White.copy(alpha = 0.07f),
    val scrim: Color = OrdoPalette.Black.copy(alpha = 0.62f),
    val danger: Color = OrdoPalette.Danger,
    val toggleTrack: Color = OrdoPalette.ToggleTrack,
)
