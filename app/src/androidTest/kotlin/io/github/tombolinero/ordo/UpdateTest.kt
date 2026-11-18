package io.github.tombolinero.ordo

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import io.github.tombolinero.ordo.core.model.ListColor
import io.github.tombolinero.ordo.core.ui.OrdoAppGraph
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * "Updates never lose your lists", checked on a real Android system:
 *   1. install the older APK, run [seed] (creates a list with a tie, a note and a picture),
 *   2. install the newer APK over it (`adb install -r`, exactly what Obtainium does),
 *   3. run [verify].
 * Driven by .github/scripts/emulator-checks.sh; skipped in normal test runs.
 */
@RunWith(AndroidJUnit4::class)
class UpdateTest {
    private val step = InstrumentationRegistry.getArguments().getString("ordoUpdateStep")
    private val graph get() = (ApplicationProvider.getApplicationContext<OrdoApp>() as OrdoAppGraph).data

    @Test
    fun seed() = runBlocking {
        assumeTrue(step == "seed")
        val list = graph.repository.createList(TITLE, "vor dem Update", ListColor.Petrol)
        val image = graph.images.save(Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.YELLOW) })
        graph.repository.addItem(list, "Erster", NOTE, image, placeIndex = 0)
        graph.repository.addItem(list, "Zweiter A", placeIndex = 1)
        graph.repository.addItem(list, "Zweiter B", placeIndex = 1, asTie = true)
        graph.repository.addItem(list, "Ablage")
        Unit
    }

    @Test
    fun verify() = runBlocking {
        assumeTrue(step == "verify")
        val summary = graph.repository.observeSummaries().first().single { it.list.title == TITLE }
        assertThat(summary.list.color).isEqualTo(ListColor.Petrol)
        val ranked = graph.repository.observeRankedList(summary.list.id).first()!!
        val places = ranked.ranking.places.map { p -> p.items.map { ranked.item(it).title }.toSet() }
        assertThat(places).containsExactly(setOf("Erster"), setOf("Zweiter A", "Zweiter B")).inOrder()
        assertThat(ranked.ranking.unranked.map { ranked.item(it).title }).containsExactly("Ablage")
        val first = ranked.items.values.single { it.title == "Erster" }
        assertThat(first.note).isEqualTo(NOTE)
        assertThat(graph.images.exists(first.image!!)).isTrue()
    }

    private companion object {
        const val TITLE = "Update-Probe"
        const val NOTE = "Diese Notiz muss das Update überleben."
    }
}
