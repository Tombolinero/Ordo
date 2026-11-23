package io.github.tombolinero.ordo

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.google.common.truth.Truth.assertThat
import io.github.tombolinero.ordo.core.designsystem.theme.OrdoTheme
import io.github.tombolinero.ordo.core.model.ImageId
import io.github.tombolinero.ordo.core.model.ItemId
import io.github.tombolinero.ordo.core.model.ListColor
import io.github.tombolinero.ordo.core.model.ListId
import io.github.tombolinero.ordo.core.ui.LocalImageCrossfade
import io.github.tombolinero.ordo.core.ui.OrdoAppGraph
import io.github.tombolinero.ordo.feature.duel.DuelRoute
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** In the duel you can look at (and zoom into) both sides' pictures before deciding. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "de-w393dp-h852dp-xxhdpi")
class DuelPicturesTest {
    @get:Rule(order = 0) val images = ImmediateImages()
    @get:Rule(order = 1) val compose = createAndroidComposeRule<ComponentActivity>()

    private val data get() = (ApplicationProvider.getApplicationContext<OrdoApp>() as OrdoAppGraph).data

    private fun picture(from: Long, to: Long): ImageId = runBlocking {
        val bmp = Bitmap.createBitmap(600, 400, Bitmap.Config.ARGB_8888)
        Canvas(bmp).drawPaint(Paint().apply { shader = LinearGradient(0f, 0f, 600f, 400f, from.toInt(), to.toInt(), Shader.TileMode.CLAMP) })
        Paint().apply { color = 0xFFFFFFFF.toInt() }.let { Canvas(bmp).drawCircle(300f, 200f, 60f, it) }
        data.images.save(bmp)
    }

    private var list = ListId("")
    private var new = ItemId("")

    private fun seed() = runBlocking {
        val repo = data.repository
        list = repo.createList("Beste E-Autos", color = ListColor.Petrol)
        repo.addItem(list, "Lucid Air", placeIndex = 0, images = listOf(picture(0xFF1E88E5, 0xFF0D1B4A), picture(0xFF5E35B1, 0xFF1A0B3D)))
        new = repo.addItem(
            list, "Porsche Taycan", "Probefahrt im März",
            images = listOf(picture(0xFFF6D818, 0xFF8A5A00), picture(0xFFE2475A, 0xFF3A0716), picture(0xFF19B3A3, 0xFF07393D)),
        )
    }

    private fun ranking() = runBlocking { data.repository.getRanking(list) }

    @Test fun lookAtPicturesBeforeDeciding() {
        seed()
        val before = ranking()
        compose.setContent {
            CompositionLocalProvider(LocalImageCrossfade provides false) { OrdoTheme { DuelRoute(list, new, onClose = {}) } }
        }
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(1_000)
        compose.onRoot().captureRoboImage("src/test/screenshots/duel_pictures.png")

        // Tapping the picture of the new entry opens the viewer and does not answer.
        compose.onAllNodesWithText("Porsche Taycan").onFirst().assertExists()
        compose.onAllNodes(hasClickLabel("Bilder ansehen")).onFirst().performClick()
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(1_000)
        assertThat(compose.onAllNodes(isDialog()).fetchSemanticsNodes()).hasSize(1)
        assertThat(ranking()).isEqualTo(before)
        captureScreenRoboImage("src/test/screenshots/duel_pictures_viewer.png")

        // Double tap zooms in.
        compose.onAllNodes(isDialog()).onFirst().performTouchInput { doubleClick(center) }
        compose.mainClock.advanceTimeBy(1_500)
        captureScreenRoboImage("src/test/screenshots/duel_pictures_zoomed.png")

        // Closing returns to the question, still unanswered; choosing then works as before.
        compose.onAllNodesWithContentDescription("Zurück").onFirst().performClick()
        compose.waitForIdle()
        assertThat(compose.onAllNodes(isDialog()).fetchSemanticsNodes()).isEmpty()
        assertThat(ranking()).isEqualTo(before)
        compose.onAllNodesWithText("Porsche Taycan").onFirst().performClick()
        compose.waitForIdle()
        compose.waitUntil(5_000) { ranking().placeOf(new) != null }
        assertThat(ranking().places.first().items.single()).isEqualTo(new)
    }
}

private fun hasClickLabel(label: String) = androidx.compose.ui.test.SemanticsMatcher("click label $label") {
    androidx.compose.ui.semantics.SemanticsActions.OnClick in it.config && it.config[androidx.compose.ui.semantics.SemanticsActions.OnClick].label == label
}
