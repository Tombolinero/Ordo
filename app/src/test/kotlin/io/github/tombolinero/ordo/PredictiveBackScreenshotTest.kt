package io.github.tombolinero.ordo

import androidx.activity.BackEventCompat
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.tombolinero.ordo.core.designsystem.theme.OrdoTheme
import io.github.tombolinero.ordo.core.model.ListColor
import io.github.tombolinero.ordo.core.ui.OrdoAppGraph
import io.github.tombolinero.ordo.navigation.OrdoNavHost
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Freezes the Android back gesture half-way (entry detail → ranking) and records it, so the
 * transition can be inspected frame by frame.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "de-w393dp-h852dp-xxhdpi")
class PredictiveBackScreenshotTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private fun seed() = runBlocking {
        val repo = (ApplicationProvider.getApplicationContext<OrdoApp>() as OrdoAppGraph).data.repository
        val list = repo.createList("Beste E-Autos", color = ListColor.Petrol)
        listOf("Lucid Air", "Rolls Royce Spectre", "Rivian R1T", "BMW i7", "Porsche Taycan", "Hyundai Ioniq 5")
            .forEachIndexed { i, t -> repo.addItem(list, t, placeIndex = i) }
    }

    private fun openItem(title: String) {
        compose.onAllNodesWithText("Beste E-Autos").onFirst().performClick()
        compose.waitForIdle()
        compose.onAllNodesWithText(title).onFirst().performClick()
        compose.waitForIdle()
    }

    @Test
    fun backGestureHalfWay() {
        seed()
        compose.setContent { OrdoTheme { OrdoNavHost() } }
        compose.waitForIdle()
        openItem("Rivian R1T")
        compose.onRoot().captureRoboImage("src/test/screenshots/back_0_detail.png")

        val dispatcher = compose.activity.onBackPressedDispatcher
        compose.mainClock.autoAdvance = false
        compose.runOnUiThread { dispatcher.dispatchOnBackStarted(BackEventCompat(10f, 900f, 0f, BackEventCompat.EDGE_LEFT)) }
        for ((i, p) in listOf(0.15f, 0.35f, 0.6f).withIndex()) {
            compose.runOnUiThread { dispatcher.dispatchOnBackProgressed(BackEventCompat(10f + p * 600f, 900f, p, BackEventCompat.EDGE_LEFT)) }
            repeat(20) { compose.mainClock.advanceTimeByFrame() }
            compose.onRoot().captureRoboImage("src/test/screenshots/back_${i + 1}_progress_${(p * 100).toInt()}.png")
        }
        compose.runOnUiThread { dispatcher.onBackPressed() }
        compose.mainClock.advanceTimeBy(3_000)
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("src/test/screenshots/back_4_done.png")
    }
}
