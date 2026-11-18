package io.github.tombolinero.ordo.core.database

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DaoTest {
    private lateinit var db: OrdoDatabase

    @Before fun open() {
        db = OrdoDatabase.create(ApplicationProvider.getApplicationContext(), name = null)
    }

    @After fun close() = db.close()

    private fun list(id: String, deletedAt: Long? = null, pinned: Boolean = false, order: Int = 0) =
        ListEntity(id, "Liste $id", "", "graphite", pinned, order, 1, 1, deletedAt)

    private fun item(id: String, list: String, title: String = id, note: String = "", place: Int? = null, deletedAt: Long? = null) =
        ItemEntity(id, list, title, note, null, place, 0, 1, 1, deletedAt)

    @Test fun `active lists exclude trash, pinned first`() = runTest {
        db.lists().upsertAll(listOf(list("a", order = 0), list("b", pinned = true, order = 1), list("c", deletedAt = 5)))
        assertThat(db.lists().observeActive().first().map { it.id }).containsExactly("b", "a").inOrder()
        assertThat(db.lists().observeDeleted().first().map { it.id }).containsExactly("c")
    }

    @Test fun `search finds titles and notes, case-insensitive, escapes wildcards`() = runTest {
        db.lists().upsert(list("l"))
        db.items().upsertAll(
            listOf(
                item("1", "l", title = "Porsche Taycan"),
                item("2", "l", title = "BMW i4", note = "fährt wie ein PORSCHE"),
                item("3", "l", title = "100% elektrisch"),
                item("4", "l", title = "Porsche 911", deletedAt = 9),
            ),
        )
        assertThat(db.items().search("%porsche%").first().map { it.item.id }).containsExactly("1", "2")
        assertThat(db.items().search("%\\%%").first().map { it.item.id }).containsExactly("3")
        assertThat(db.items().search("%porsche%").first().first().listTitle).isEqualTo("Liste l")
    }

    @Test fun `deleting a list forever cascades to its entries`() = runTest {
        db.lists().upsert(list("l"))
        db.items().upsertAll(listOf(item("1", "l"), item("2", "l")))
        db.lists().deleteForever("l")
        assertThat(db.items().getAll()).isEmpty()
    }

    @Test fun `purge removes only old trash`() = runTest {
        db.lists().upsertAll(listOf(list("old", deletedAt = 10), list("new", deletedAt = 1_000), list("live")))
        db.items().upsertAll(listOf(item("x", "live", deletedAt = 10), item("y", "live", deletedAt = 1_000), item("z", "live")))
        db.items().purgeDeletedBefore(500)
        db.lists().purgeDeletedBefore(500)
        assertThat(db.lists().getAll().map { it.id }).containsExactly("new", "live")
        assertThat(db.items().getAll().map { it.id }).containsExactly("y", "z")
    }

    @Test fun `positions are written in one transaction`() = runTest {
        db.lists().upsert(list("l"))
        db.items().upsertAll(listOf(item("1", "l"), item("2", "l")))
        db.items().setPositions(listOf(Triple("1", 0, 0), Triple("2", 0, 1)))
        assertThat(db.items().getActiveOfList("l").map { it.place }).containsExactly(0, 0)
    }
}
