package io.github.tombolinero.ordo.core.designsystem.motion

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring

/**
 * All motion in Ordo is spring based: animations can be interrupted at any time and
 * continue from their current velocity instead of restarting. No fixed-duration tweens.
 */
object OrdoMotion {
    /** Default for layout changes: settles quickly, almost no overshoot. */
    fun <T> smooth(visibilityThreshold: T? = null): SpringSpec<T> =
        spring(dampingRatio = 0.9f, stiffness = 380f, visibilityThreshold = visibilityThreshold)

    /** Taps, presses, toggles: fast and crisp. */
    fun <T> snappy(visibilityThreshold: T? = null): SpringSpec<T> =
        spring(dampingRatio = 0.78f, stiffness = 700f, visibilityThreshold = visibilityThreshold)

    /** Something lands or is picked up: a visible but soft bounce. */
    fun <T> bouncy(visibilityThreshold: T? = null): SpringSpec<T> =
        spring(dampingRatio = 0.62f, stiffness = 420f, visibilityThreshold = visibilityThreshold)

    /** Large surfaces (sheets, screen transitions): slow and weighty, like water. */
    fun <T> fluid(visibilityThreshold: T? = null): SpringSpec<T> =
        spring(dampingRatio = 0.86f, stiffness = Spring.StiffnessLow, visibilityThreshold = visibilityThreshold)

    const val PressedScale = 0.96f
    const val LiftedScale = 1.035f
}
