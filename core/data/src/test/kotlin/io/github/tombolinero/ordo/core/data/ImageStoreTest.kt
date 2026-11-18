package io.github.tombolinero.ordo.core.data

import android.graphics.Bitmap
import android.graphics.Color
import androidx.exifinterface.media.ExifInterface
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.tombolinero.ordo.core.model.ImageId
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ImageStoreTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun bitmap(w: Int, h: Int) = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }

    @Test fun `rotates per exif, then crops`() {
        val out = ImageStore.process(bitmap(400, 200), ExifInterface.ORIENTATION_ROTATE_90, CropRect(0f, 0f, 1f, 0.5f))
        assertThat(out.width).isEqualTo(200)
        assertThat(out.height).isEqualTo(200)
    }

    @Test fun `large photos are scaled to 2048 px on the long side`() {
        val out = ImageStore.process(bitmap(4096, 3072), ExifInterface.ORIENTATION_NORMAL, CropRect.Full)
        assertThat(out.width).isEqualTo(2048)
        assertThat(out.height).isEqualTo(1536)
        assertThat(ImageStore.sampleSize(8000, 6000)).isEqualTo(2)
    }

    @Test fun `saves webp and cleans up unreferenced files`() = runTest {
        val store = ImageStore(tmp.newFolder())
        val keep = store.save(bitmap(10, 10))
        val drop = store.save(bitmap(10, 10))
        assertThat(store.exists(keep)).isTrue()
        // Fresh files survive a cleanup (they may be about to be referenced) ...
        store.deleteAllExcept(setOf(keep))
        assertThat(store.exists(drop)).isTrue()
        // ... once they are older than the grace period, unreferenced ones go.
        store.deleteAllExcept(setOf(keep), now = System.currentTimeMillis() + ImageStore.CLEANUP_GRACE_MILLIS + 1)
        assertThat(store.exists(keep)).isTrue()
        assertThat(store.exists(drop)).isFalse()
    }

    @Test fun `an image saved while a cleanup runs is not lost`() = runTest {
        val store = ImageStore(tmp.newFolder())
        val fresh = store.save(bitmap(10, 10))
        store.deleteAllExcept(emptySet())
        assertThat(store.exists(fresh)).isTrue()
    }

    @Test fun `adopt rejects unsafe names`() {
        val store = ImageStore(tmp.newFolder())
        val src = tmp.newFile("x")
        assertThat(store.adopt("../../evil.webp", src)).isFalse()
        assertThat(store.adopt("ok.webp", src)).isTrue()
        assertThat(store.exists(ImageId("ok"))).isTrue()
    }
}
