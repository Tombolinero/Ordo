package io.github.tombolinero.ordo.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.tombolinero.ordo.core.designsystem.motion.OrdoMotion
import io.github.tombolinero.ordo.core.ui.LocalNavAnimatedVisibilityScope
import io.github.tombolinero.ordo.core.ui.LocalSharedTransitionScope
import io.github.tombolinero.ordo.feature.lists.HomeRoute

/**
 * Screen transitions (identical for the back button and the back gesture, which follows the
 * finger): the new screen glides in from the right on a soft spring while the
 * old one drifts a little to the left and dims; going back mirrors it (and follows the
 * finger with predictive back). Elements marked as shared morph between the screens.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun OrdoNavHost(openList: String? = null, onListOpened: () -> Unit = {}) {
    val nav = rememberNavController()
    LaunchedEffect(openList) {
        if (openList != null) {
            nav.navigate(ListDestination(openList)) { popUpTo(HomeDestination) }
            onListOpened()
        }
    }
    SharedTransitionLayout {
        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
            NavHost(
                navController = nav,
                startDestination = HomeDestination,
                enterTransition = { enter() },
                exitTransition = { exit() },
                popEnterTransition = { popEnter() },
                popExitTransition = { popExit() },
                // The Android back gesture uses its own transition slots; without these the
                // library default shrinks the page into a small window while you swipe.
                predictivePopEnterTransition = { gestureEnter() },
                predictivePopExitTransition = { gestureExit() },
            ) {
                screen<HomeDestination> {
                    HomeRoute(
                        onOpenList = { nav.navigate(ListDestination(it.value)) },
                        onOpenSearch = { nav.navigate(SearchDestination) },
                        onOpenSettings = { nav.navigate(SettingsDestination) },
                    )
                }
                destinations(nav)
            }
        }
    }
}

/** Registers a destination and exposes its animation scope for shared elements. */
inline fun <reified T : Any> NavGraphBuilder.screen(noinline content: @Composable (NavBackStackEntry) -> Unit) {
    composable<T> { entry ->
        CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) { content(entry) }
    }
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.enter(): EnterTransition =
    slideInHorizontally(OrdoMotion.fluid(IntOffset.VisibilityThreshold)) { it } + fadeIn(OrdoMotion.smooth())

private fun AnimatedContentTransitionScope<NavBackStackEntry>.exit(): ExitTransition =
    slideOutHorizontally(OrdoMotion.fluid(IntOffset.VisibilityThreshold)) { -it / 4 } + fadeOut(OrdoMotion.smooth(), targetAlpha = 0.4f)

private fun AnimatedContentTransitionScope<NavBackStackEntry>.popEnter(): EnterTransition =
    slideInHorizontally(OrdoMotion.fluid(IntOffset.VisibilityThreshold)) { -it / 4 } + fadeIn(OrdoMotion.smooth(), initialAlpha = 0.4f)

private fun AnimatedContentTransitionScope<NavBackStackEntry>.popExit(): ExitTransition =
    slideOutHorizontally(OrdoMotion.fluid(IntOffset.VisibilityThreshold)) { it } +
        scaleOut(OrdoMotion.fluid(), targetScale = 0.96f) + fadeOut(OrdoMotion.smooth())

// Back gesture: the page stays opaque and follows the finger to the right, the screen below
// glides in from slightly left. No cross-fade, so the two screens never show through each other.
private fun AnimatedContentTransitionScope<NavBackStackEntry>.gestureEnter(): EnterTransition =
    slideInHorizontally(OrdoMotion.fluid(IntOffset.VisibilityThreshold)) { -it / 5 }

private fun AnimatedContentTransitionScope<NavBackStackEntry>.gestureExit(): ExitTransition =
    slideOutHorizontally(OrdoMotion.fluid(IntOffset.VisibilityThreshold)) { it }
