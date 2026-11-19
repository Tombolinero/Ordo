package io.github.tombolinero.ordo.feature.ranking

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.net.Uri
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.google.common.truth.Truth.assertThat
import io.github.tombolinero.ordo.core.data.CropRect
import io.github.tombolinero.ordo.core.data.DataGraph
import io.github.tombolinero.ordo.core.designsystem.theme.OrdoTheme
import io.github.tombolinero.ordo.core.ui.PictureDraft
import io.github.tombolinero.ordo.core.ui.PicturesState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** A new entry can get several pictures right away, one of them the cover. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "de-w393dp-h852dp-xxhdpi")
class AddItemTest {
    @get:Rule(order = 0) val images = ImmediateImages()
    @get:Rule(order = 1) val compose = createComposeRule()
    private lateinit var graph: DataGraph

    @Before fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        graph = DataGraph(ApplicationProvider.getApplicationContext(), "test", 1, databaseName = null)
    }

    @After fun tearDown() {
        graph.close()
        Dispatchers.resetMain()
    }

    private fun pictureFile(name: String, from: Int, to: Int): Uri {
        val bmp = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888)
        Canvas(bmp).drawPaint(Paint().apply { shader = LinearGradient(0f, 0f, 400f, 400f, from, to, Shader.TileMode.CLAMP) })
        val f = File(ApplicationProvider.getApplicationContext<android.content.Context>().cacheDir, "$name.png")
        f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return Uri.fromFile(f)
    }

    private fun drafts() = listOf(
        PictureDraft.Picked(pictureFile("front", 0xFF1E88E5.toInt(), 0xFF0D1B4A.toInt()), CropRect.Full),
        PictureDraft.Picked(pictureFile("side", 0xFFF6D818.toInt(), 0xFF8A5A00.toInt()), CropRect(0f, 0f, 0.5f, 0.5f)),
        PictureDraft.Picked(pictureFile("back", 0xFFE2475A.toInt(), 0xFF3A0716.toInt()), CropRect.Full),
    )

    @Test fun `new entry is saved with all pictures and the chosen cover`() = runBlocking<Unit> {
        val list = graph.repository.createList("Autos")
        val vm = RankingViewModel(graph, list)
        vm.addItem(ApplicationProvider.getApplicationContext(), "Taycan", "", drafts(), cover = 1, placeNow = false) {}
        val item = withTimeout(5_000) {
            var found = graph.repository.observeRankedList(list).first()?.items?.values?.firstOrNull()
            while (found == null) { delay(10); found = graph.repository.observeRankedList(list).first()?.items?.values?.firstOrNull() }
            found
        }
        assertThat(item.images).hasSize(3)
        assertThat(item.image).isEqualTo(item.images[1])
        item.images.forEach { assertThat(graph.images.file(it).exists()).isTrue() }
    }

    @Test fun sheetWithPictures() {
        val state = PicturesState(drafts(), initialCover = 1)
        compose.setContent { OrdoTheme { AddItemSheet(onDismiss = {}, pictures = state) { _, _, _, _, _ -> } } }
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(1_500)
        captureScreenRoboImage("src/test/screenshots/add_item_pictures.png")
    }

    @Test fun sheetEmpty() {
        compose.setContent { OrdoTheme { AddItemSheet(onDismiss = {}) { _, _, _, _, _ -> } } }
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(1_500)
        captureScreenRoboImage("src/test/screenshots/add_item_empty.png")
    }
}
