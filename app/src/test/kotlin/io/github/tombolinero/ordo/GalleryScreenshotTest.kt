package io.github.tombolinero.ordo

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
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
import io.github.tombolinero.ordo.core.ui.OrdoAppGraph
import io.github.tombolinero.ordo.navigation.OrdoNavHost
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Several pictures per entry: detail page, viewer and choosing another cover. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "de-w393dp-h852dp-xxhdpi")
class GalleryScreenshotTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val data get() = (ApplicationProvider.getApplicationContext<OrdoApp>() as OrdoAppGraph).data

    private fun picture(from: Int, to: Int): ImageId = runBlocking {
        val bmp = Bitmap.createBitmap(600, 600, Bitmap.Config.ARGB_8888)
        Canvas(bmp).drawPaint(Paint().apply { shader = LinearGradient(0f, 0f, 600f, 600f, from, to, Shader.TileMode.CLAMP) })
        data.images.save(bmp)
    }

    private var list = ListId("")
    private var item = ItemId("")
    private var pics = emptyList<ImageId>()

    private fun seed() = runBlocking {
        val repo = data.repository
        list = repo.createList("Beste E-Autos", color = ListColor.Petrol)
        pics = listOf(
            picture(0xFF1E88E5.toInt(), 0xFF0D1B4A.toInt()),
            picture(0xFFF6D818.toInt(), 0xFF8A5A00.toInt()),
            picture(0xFFE2475A.toInt(), 0xFF3A0716.toInt()),
        )
        repo.addItem(list, "Lucid Air", placeIndex = 0)
        item = repo.addItem(list, "Porsche Taycan", placeIndex = 1, images = pics)
        repo.addItem(list, "BMW i7", placeIndex = 2)
    }

    @Test fun galleryOnEntryPage() {
        seed()
        compose.setContent { OrdoTheme { OrdoNavHost() } }
        compose.waitForIdle()
        compose.onAllNodesWithText("Beste E-Autos").onFirst().performClick()
        compose.waitForIdle()
        compose.onAllNodesWithText("Porsche Taycan").onFirst().performClick()
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(1_000)
        compose.onRoot().captureRoboImage("src/test/screenshots/gallery_detail.png")

        // Edit: tap the third picture to make it the cover, then save.
        compose.onAllNodesWithContentDescription("Mehr").onFirst().performClick()
        compose.waitForIdle()
        compose.onAllNodesWithText("Bearbeiten").onFirst().performClick()
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(1_000)
        compose.onAllNodesWithContentDescription("Bild entfernen").assertCountEquals(3)
        captureScreenRoboImage("src/test/screenshots/gallery_edit.png")
    }

    @Test fun viewerAndCoverChange() {
        seed()
        runBlocking { data.repository.updateItem(item, "Porsche Taycan", "", pics[2], pics) }
        val stored = runBlocking { data.repository.observeRankedList(list).first()!!.item(item) }
        assertThat(stored.image).isEqualTo(pics[2])
        assertThat(stored.images).containsExactlyElementsIn(pics).inOrder()

        compose.setContent { OrdoTheme { OrdoNavHost() } }
        compose.waitForIdle()
        compose.onAllNodesWithText("Beste E-Autos").onFirst().performClick()
        compose.waitForIdle()
        compose.onAllNodesWithText("Porsche Taycan").onFirst().performClick()
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(1_000)
        compose.onRoot().captureRoboImage("src/test/screenshots/gallery_cover_changed.png")
    }
}

private fun androidx.compose.ui.test.SemanticsNodeInteractionCollection.assertCountEquals(n: Int) =
    assertThat(fetchSemanticsNodes().size).isEqualTo(n)
