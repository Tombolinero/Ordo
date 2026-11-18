package io.github.tombolinero.ordo.core.designsystem.theme

import androidx.compose.foundation.LocalIndication
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LocalOrdoColors = staticCompositionLocalOf { OrdoColors() }
private val LocalOrdoTypography = staticCompositionLocalOf { OrdoTypography() }

object OrdoTheme {
    val colors: OrdoColors
        @Composable @ReadOnlyComposable get() = LocalOrdoColors.current
    val type: OrdoTypography
        @Composable @ReadOnlyComposable get() = LocalOrdoTypography.current
}

/** Ordo is dark only, as in the reference design. */
@Composable
fun OrdoTheme(content: @Composable () -> Unit) {
    val colors = OrdoColors()
    val type = OrdoTypography()
    val material = darkColorScheme(
        primary = colors.accent,
        onPrimary = colors.onAccent,
        background = colors.background,
        onBackground = colors.textPrimary,
        surface = OrdoPalette.Graphite850,
        onSurface = colors.textPrimary,
        surfaceVariant = OrdoPalette.Graphite800,
        onSurfaceVariant = colors.textSecondary,
        surfaceContainerHighest = OrdoPalette.Graphite800,
        error = colors.danger,
        outline = colors.divider,
    )
    MaterialTheme(colorScheme = material) {
        CompositionLocalProvider(
            LocalOrdoColors provides colors,
            LocalOrdoTypography provides type,
            LocalIndication provides ripple(color = Color.White.copy(alpha = 0.12f)),
            content = content,
        )
    }
}
