package io.github.tombolinero.ordo.core.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.tombolinero.ordo.core.database.OrdoDatabase
import io.github.tombolinero.ordo.core.model.Clock
import io.github.tombolinero.ordo.core.model.ImageId
import io.github.tombolinero.ordo.core.model.ListColor
import io.github.tombolinero.ordo.core.model.Retention
import io.github.tombolinero.ordo.core.model.TrashEntry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OrdoRepositoryTest {
    @get:Rule val tmp = TemporaryFolder()

    private var now = 1_000_000L
    private val clock = Clock { now }
    private lateinit var db: OrdoDatabase
    private lateinit var images: ImageStore
    private lateinit var repo: OrdoRepository

    @Before fun setUp() {
        db = OrdoDatabase.create(ApplicationProvider.getApplicationContext(), name = null)
        images = ImageStore(tmp.newFolder("images"))
        repo = OrdoRepository(db, images, clock)
    }

    @After fun tearDown() = db.close()

    @Test fun `new entries land in the inbox, placing them numbers densely`() = runTest {
        val list = repo.createList("Beste Autos", color = ListColor.Magenta)
        val a = repo.addItem(list, "Koenigsegg", placeIndex = 0)
        val b = repo.addItem(list, "Taycan", placeIndex = 1)
        val c = repo.addItem(list, "Ioniq")
        var ranked = repo.observeRankedList(list).first()!!
        assertThat(ranked.ranking.unranked).containsExactly(c)
        assertThat(ranked.ranking.placeOf(b)).isEqualTo(2)

        repo.saveRanking(list, ranked.ranking.place(c, 1, asTie = true))
        ranked = repo.observeRankedList(list).first()!!
        assertThat(ranked.ranking.placeOf(c)).isEqualTo(2)
        assertThat(ranked.ranking.placeOf(b)).isEqualTo(2)
        assertThat(ranked.ranking.placeOf(a)).isEqualTo(1)
    }

    @Test fun `cover always belongs to the pictures`() = runTest {
        val list = repo.createList("L")
        val (p1, p2, p3) = listOf(ImageId("p1"), ImageId("p2"), ImageId("p3"))
        // No cover chosen: the first picture becomes the cover.
        val a = repo.addItem(list, "A", images = listOf(p1, p2))
        suspend fun item() = repo.observeRankedList(list).first()!!.item(a)
        assertThat(item().image).isEqualTo(p1)

        // Changing only the cover (old screens) keeps the other pictures in their slots.
        repo.updateItem(a, "A", "", p3)
        assertThat(item().images).containsExactly(p3, p2).inOrder()

        // Removing the cover without a picture list removes just that picture.
        repo.updateItem(a, "A", "", null)
        assertThat(item().images).containsExactly(p2)
        assertThat(item().image).isEqualTo(p2)

        // Choosing another cover from the full list.
        repo.updateItem(a, "A", "", p1, images = listOf(p1, p2))
        assertThat(item().image).isEqualTo(p1)
        assertThat(item().images).containsExactly(p1, p2).inOrder()

        repo.updateItem(a, "A", "", null, images = emptyList())
        assertThat(item().image).isNull()
        assertThat(item().images).isEmpty()
    }

    @Test fun `trash and restore puts the entry back on its old place`() = runTest {
        val list = repo.createList("L")
        val ids = (1..4).map { repo.addItem(list, "E$it", placeIndex = it - 1) }
        repo.moveItemToTrash(ids[1])
        var ranking = repo.observeRankedList(list).first()!!.ranking
        assertThat(ranking.placeOf(ids[2])).isEqualTo(2)
        assertThat(repo.observeTrash().first().filterIsInstance<TrashEntry.Item>().map { it.item.id }).containsExactly(ids[1])

        repo.restoreItem(ids[1])
        ranking = repo.observeRankedList(list).first()!!.ranking
        assertThat(ranking.places.map { it.items.single() }).containsExactlyElementsIn(ids).inOrder()
    }

    @Test fun `saving a ranking that does not match the list is refused`() = runTest {
        val list = repo.createList("L")
        val a = repo.addItem(list, "A", placeIndex = 0)
        repo.addItem(list, "B", placeIndex = 1)
        val partial = io.github.tombolinero.ordo.core.model.Ranking(listOf(io.github.tombolinero.ordo.core.model.Ranking.Place(listOf(a))))
        val error = runCatching { repo.saveRanking(list, partial) }.exceptionOrNull()
        assertThat(error).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test fun `summaries show counts, ties and the top entry`() = runTest {
        val list = repo.createList("L")
        val a = repo.addItem(list, "A", placeIndex = 0)
        repo.addItem(list, "B", placeIndex = 1)
        repo.addItem(list, "C", placeIndex = 1, asTie = true)
        repo.addItem(list, "D")
        val s = repo.observeSummaries().first().single()
        assertThat(s.itemCount).isEqualTo(4)
        assertThat(s.tieCount).isEqualTo(1)
        assertThat(s.unrankedCount).isEqualTo(1)
        assertThat(s.topItem?.id).isEqualTo(a)
    }

    @Test fun `new lists appear on top, pinned lists stay above`() = runTest {
        val first = repo.createList("Erste")
        val second = repo.createList("Zweite")
        assertThat(repo.observeSummaries().first().map { it.list.id }).containsExactly(second, first).inOrder()
        repo.setPinned(first, true)
        assertThat(repo.observeSummaries().first().map { it.list.id }).containsExactly(first, second).inOrder()
    }

    @Test fun `search finds entries across lists with their place`() = runTest {
        val cars = repo.createList("Autos")
        val names = repo.createList("Namen")
        repo.addItem(cars, "Porsche Taycan", placeIndex = 0)
        repo.addItem(names, "Emma", note = "wie die Porsche-Fahrerin")
        val hits = repo.search("porsche").first()
        assertThat(hits.map { it.item.title }).containsExactly("Emma", "Porsche Taycan")
        assertThat(hits.first { it.item.title == "Porsche Taycan" }.place).isEqualTo(1)
        assertThat(hits.first { it.item.title == "Emma" }.place).isNull()
        assertThat(repo.search("  ").first()).isEmpty()
    }

    @Test fun `duplicate copies entries and order with new ids`() = runTest {
        val list = repo.createList("Original")
        repo.addItem(list, "A", placeIndex = 0)
        repo.addItem(list, "B", placeIndex = 0, asTie = true)
        val copy = repo.duplicateList(list, "(Kopie)")
        val ranked = repo.observeRankedList(copy).first()!!
        assertThat(ranked.list.title).isEqualTo("Original (Kopie)")
        assertThat(ranked.ranking.places.single().items).hasSize(2)
        assertThat(ranked.items.keys).containsNoneIn(repo.observeRankedList(list).first()!!.items.keys)
    }

    @Test fun `trash is purged after 30 days, not earlier`() = runTest {
        val list = repo.createList("L")
        val gone = repo.createList("Weg")
        val item = repo.addItem(list, "A")
        repo.moveItemToTrash(item)
        repo.moveListToTrash(gone)
        now += Retention.TRASH_MILLIS - 1
        repo.purgeExpired()
        assertThat(repo.observeTrash().first()).hasSize(2)
        now += 2
        repo.purgeExpired()
        assertThat(repo.observeTrash().first()).isEmpty()
    }

    @Test fun `trashed list hides from home and restores with its entries`() = runTest {
        val list = repo.createList("L")
        repo.addItem(list, "A", placeIndex = 0)
        repo.moveListToTrash(list)
        assertThat(repo.observeSummaries().first()).isEmpty()
        assertThat(repo.observeRankedList(list).first()).isNull()
        repo.restoreList(list)
        assertThat(repo.observeRankedList(list).first()!!.items).hasSize(1)
    }
}
