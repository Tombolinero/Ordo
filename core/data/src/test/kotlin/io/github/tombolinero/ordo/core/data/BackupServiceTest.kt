package io.github.tombolinero.ordo.core.data

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.tombolinero.ordo.core.backup.BackupException
import io.github.tombolinero.ordo.core.database.OrdoDatabase
import io.github.tombolinero.ordo.core.model.Clock
import io.github.tombolinero.ordo.core.model.ListColor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayOutputStream

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BackupServiceTest {
    @get:Rule val tmp = TemporaryFolder()

    private var now = 1_700_000_000_000L
    private val clock = Clock { now }

    /** One "phone": its own database, image folder and services. */
    private inner class Phone(name: String) {
        val db = OrdoDatabase.create(ApplicationProvider.getApplicationContext(), name = null)
        val images = ImageStore(tmp.newFolder("$name-images"))
        val repo = OrdoRepository(db, images, clock)
        val backups = BackupService(db, images, tmp.newFolder("$name-work"), "1.0.0", 10000, clock)
        val safety = tmp.newFolder("$name-safety")

        fun backup(): ByteArray = ByteArrayOutputStream().also { kotlinx.coroutines.runBlocking { backups.export(it) } }.toByteArray()
    }

    private lateinit var old: Phone
    private lateinit var fresh: Phone

    @Before fun setUp() {
        old = Phone("old")
        fresh = Phone("new")
    }

    @After fun tearDown() {
        old.db.close(); fresh.db.close()
    }

    private suspend fun Phone.sampleImage() =
        images.save(Bitmap.createBitmap(40, 30, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.YELLOW) })

    @Test fun `phone change - everything arrives on a clean install`() = runTest {
        val cars = old.repo.createList("Beste Autos", "Favoriten", ListColor.Ruby)
        val img = old.sampleImage()
        val a = old.repo.addItem(cars, "Koenigsegg", "Über 1.600 PS", img, placeIndex = 0)
        old.repo.addItem(cars, "Taycan", placeIndex = 1)
        old.repo.addItem(cars, "Ioniq", placeIndex = 1, asTie = true)
        old.repo.addItem(cars, "Polestar")
        val trashed = old.repo.addItem(cars, "Alt", placeIndex = 3)
        old.repo.moveItemToTrash(trashed)
        old.repo.setPinned(cars, true)

        val prepared = fresh.backups.prepare(old.backup().inputStream())
        assertThat(prepared.summary.lists).isEqualTo(1)
        assertThat(prepared.summary.items).isEqualTo(4)
        fresh.backups.apply(prepared, BackupService.Mode.Replace, fresh.safety)

        val ranked = fresh.repo.observeRankedList(cars).first()!!
        assertThat(ranked).isEqualTo(old.repo.observeRankedList(cars).first()!!)
        assertThat(ranked.item(a).note).isEqualTo("Über 1.600 PS")
        assertThat(fresh.images.file(img).readBytes()).isEqualTo(old.images.file(img).readBytes())
        assertThat(fresh.repo.observeTrash().first()).hasSize(1)
    }

    @Test fun `several pictures per entry survive backup and merge`() = runTest {
        val cars = old.repo.createList("Autos")
        val front = old.sampleImage()
        val side = old.sampleImage()
        val back = old.sampleImage()
        val a = old.repo.addItem(cars, "Taycan", image = side, placeIndex = 0, images = listOf(front, side, back))

        fresh.backups.apply(fresh.backups.prepare(old.backup().inputStream()), BackupService.Mode.Replace, fresh.safety)
        val item = fresh.repo.observeRankedList(cars).first()!!.item(a)
        assertThat(item.image).isEqualTo(side)
        assertThat(item.images).containsExactly(front, side, back).inOrder()
        listOf(front, side, back).forEach { assertThat(fresh.images.file(it).exists()).isTrue() }

        // Cleanup must keep gallery pictures, not only covers.
        fresh.repo.cleanupImages()
        listOf(front, side, back).forEach { assertThat(fresh.images.file(it).exists()).isTrue() }

        // A newer edit with fewer pictures wins on merge.
        now += 1000
        old.repo.updateItem(a, "Taycan", "", front, images = listOf(front))
        fresh.backups.apply(fresh.backups.prepare(old.backup().inputStream()), BackupService.Mode.Merge, fresh.safety)
        val merged = fresh.repo.observeRankedList(cars).first()!!.item(a)
        assertThat(merged.image).isEqualTo(front)
        assertThat(merged.images).containsExactly(front)
    }

    @Test fun `replace removes what is not in the backup, safety backup allows going back`() = runTest {
        old.repo.createList("Aus dem Backup")
        val local = fresh.repo.createList("Nur lokal")
        fresh.repo.addItem(local, "Wichtig")

        fresh.backups.apply(fresh.backups.prepare(old.backup().inputStream()), BackupService.Mode.Replace, fresh.safety)
        assertThat(fresh.repo.observeSummaries().first().map { it.list.title }).containsExactly("Aus dem Backup")

        val safety = fresh.safety.listFiles()!!.single()
        fresh.backups.apply(fresh.backups.prepare(safety.inputStream()), BackupService.Mode.Replace, fresh.safety)
        assertThat(fresh.repo.observeSummaries().first().map { it.list.title }).containsExactly("Nur lokal")
    }

    @Test fun `merge adds new lists and entries, newer edits win, local order stays`() = runTest {
        val shared = old.repo.createList("Geteilt")
        val x = old.repo.addItem(shared, "X", placeIndex = 0)
        val y = old.repo.addItem(shared, "Y", placeIndex = 1)
        fresh.backups.apply(fresh.backups.prepare(old.backup().inputStream()), BackupService.Mode.Replace, fresh.safety)

        // Diverge: locally swap X and Y; in the backup rename X and add Z plus a new list.
        val local = fresh.repo.getRanking(shared)
        fresh.repo.saveRanking(shared, local.movePlace(1, 0))
        now += 1000
        old.repo.updateItem(x, "X neu", "", null)
        val z = old.repo.addItem(shared, "Z", placeIndex = 0)
        old.repo.createList("Neue Liste")

        fresh.backups.apply(fresh.backups.prepare(old.backup().inputStream()), BackupService.Mode.Merge, fresh.safety)
        val merged = fresh.repo.observeRankedList(shared).first()!!
        assertThat(merged.ranking.places.map { it.items.single() }).containsExactly(y, x).inOrder()
        assertThat(merged.ranking.unranked).containsExactly(z)
        assertThat(merged.item(x).title).isEqualTo("X neu")
        assertThat(fresh.repo.observeSummaries().first().map { it.list.title }).contains("Neue Liste")
    }

    @Test fun `a damaged file changes nothing`() = runTest {
        val list = fresh.repo.createList("Bleibt")
        val bytes = old.backup()
        val damaged = bytes.copyOf(bytes.size / 2)
        val error = runCatching { fresh.backups.prepare(damaged.inputStream()) }.exceptionOrNull()
        assertThat(error).isInstanceOf(BackupException::class.java)
        assertThat(fresh.repo.observeSummaries().first().single().list.id).isEqualTo(list)
    }
}
