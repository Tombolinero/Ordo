package io.github.tombolinero.ordo.core.data

import androidx.room.withTransaction
import io.github.tombolinero.ordo.core.database.ItemEntity
import io.github.tombolinero.ordo.core.database.OrdoDatabase
import io.github.tombolinero.ordo.core.model.Clock
import io.github.tombolinero.ordo.core.model.ImageId
import io.github.tombolinero.ordo.core.model.ItemId
import io.github.tombolinero.ordo.core.model.ListColor
import io.github.tombolinero.ordo.core.model.ListId
import io.github.tombolinero.ordo.core.model.ListSummary
import io.github.tombolinero.ordo.core.model.RankItem
import io.github.tombolinero.ordo.core.model.RankList
import io.github.tombolinero.ordo.core.model.RankedList
import io.github.tombolinero.ordo.core.model.Ranking
import io.github.tombolinero.ordo.core.model.Retention
import io.github.tombolinero.ordo.core.model.SearchHit
import io.github.tombolinero.ordo.core.model.TrashEntry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Single entry point for reading and changing lists and entries.
 * All writes that touch several rows run in one database transaction.
 */
class OrdoRepository(
    private val db: OrdoDatabase,
    private val images: ImageStore,
    private val clock: Clock = Clock.System,
) {
    private val lists = db.lists()
    private val items = db.items()

    // ---------------------------------------------------------------- reading

    fun observeSummaries(): Flow<List<ListSummary>> =
        combine(lists.observeActive(), items.observeAllActive()) { listRows, itemRows ->
            val byList = itemRows.groupBy { it.listId }
            listRows.map { l ->
                val rows = byList[l.id].orEmpty()
                val ranking = rows.toRanking()
                val itemsById = rows.associateBy { it.id }
                val placedImages = ranking.places.flatMap { it.items }
                    .mapNotNull { itemsById[it.value]?.imageId }
                    .take(4)
                    .map(::ImageId)
                ListSummary(
                    list = l.toModel(),
                    itemCount = rows.size,
                    tieCount = ranking.places.count { it.isTie },
                    unrankedCount = ranking.unranked.size,
                    topItem = ranking.places.firstOrNull()?.items?.firstOrNull()?.let { itemsById[it.value]?.toModel() },
                    previewImages = placedImages,
                )
            }
        }.distinctUntilChanged()

    fun observeRankedList(id: ListId): Flow<RankedList?> =
        combine(lists.observe(id.value), items.observeActiveOfList(id.value)) { l, rows ->
            if (l == null || l.deletedAt != null) {
                null
            } else {
                RankedList(l.toModel(), rows.associate { ItemId(it.id) to it.toModel() }, rows.toRanking())
            }
        }.distinctUntilChanged()

    fun observeItem(id: ItemId): Flow<RankItem?> = items.observe(id.value).map { it?.takeIf { e -> e.deletedAt == null }?.toModel() }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun search(query: String): Flow<List<SearchHit>> {
        val q = query.trim()
        if (q.isEmpty()) return flowOf(emptyList())
        val pattern = "%" + q.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%"
        return items.search(pattern).flatMapLatest { rows ->
            val listIds = rows.map { it.item.listId }.distinct()
            if (listIds.isEmpty()) return@flatMapLatest flowOf(emptyList())
            combine(listIds.map { lid -> combine(lists.observe(lid), items.observeActiveOfList(lid)) { l, r -> l to r.toRanking() } }) { arr ->
                val info = arr.associateBy { it.first?.id }
                rows.mapNotNull { row ->
                    val (l, ranking) = info[row.item.listId] ?: return@mapNotNull null
                    if (l == null) return@mapNotNull null
                    SearchHit(row.item.toModel(), l.toModel(), ranking.placeOf(ItemId(row.item.id)))
                }
            }
        }
    }

    fun observeTrash(): Flow<List<TrashEntry>> =
        combine(lists.observeDeleted(), items.observeDeleted(), items.observeAllActive()) { deletedLists, deletedItems, _ ->
            val listEntries = deletedLists.map { l -> TrashEntry.List(l.toModel(), items.getAllOfList(l.id).size) }
            val itemEntries = deletedItems.map { TrashEntry.Item(it.item.toModel(), it.listTitle) }
            (listEntries + itemEntries).sortedByDescending { it.deletedAt }
        }

    // ---------------------------------------------------------------- lists

    suspend fun createList(title: String, description: String = "", color: ListColor = ListColor.Graphite): ListId {
        val now = clock.now()
        val id = ListId.new()
        lists.upsert(
            RankList(id, title.trim(), description.trim(), color, pinned = false, sortOrder = lists.minSortOrder() - 1, createdAt = now, updatedAt = now).toEntity(),
        )
        return id
    }

    suspend fun updateList(id: ListId, title: String, description: String, color: ListColor) {
        val current = lists.get(id.value) ?: return
        lists.upsert(current.copy(title = title.trim(), description = description.trim(), color = color.key, updatedAt = clock.now()))
    }

    suspend fun setPinned(id: ListId, pinned: Boolean) {
        val current = lists.get(id.value) ?: return
        lists.upsert(current.copy(pinned = pinned, updatedAt = clock.now()))
    }

    /** Persists the home screen order (top first). */
    suspend fun reorderLists(order: List<ListId>) = db.withTransaction {
        order.forEachIndexed { i, id -> lists.setSortOrder(id.value, i) }
    }

    /** Copies a list with all its live entries; images are shared, not copied. */
    suspend fun duplicateList(id: ListId, copySuffix: String): ListId = db.withTransaction {
        val source = lists.get(id.value) ?: error("List not found")
        val now = clock.now()
        val newId = ListId.new()
        lists.upsert(source.copy(id = newId.value, title = "${source.title} $copySuffix".trim(), pinned = false, sortOrder = lists.minSortOrder() - 1, createdAt = now, updatedAt = now, deletedAt = null))
        items.upsertAll(items.getActiveOfList(id.value).map { it.copy(id = ItemId.new().value, listId = newId.value, createdAt = now, updatedAt = now) })
        newId
    }

    suspend fun moveListToTrash(id: ListId) = lists.setDeleted(id.value, clock.now(), clock.now())

    suspend fun restoreList(id: ListId) = lists.setDeleted(id.value, null, clock.now())

    // ---------------------------------------------------------------- entries

    /** Adds an entry. It lands in the inbox ("not placed yet") unless [placeIndex] is given. */
    suspend fun addItem(
        listId: ListId,
        title: String,
        note: String = "",
        image: ImageId? = null,
        placeIndex: Int? = null,
        asTie: Boolean = false,
        images: List<ImageId> = listOfNotNull(image),
    ): ItemId =
        db.withTransaction {
            val now = clock.now()
            val id = ItemId.new()
            val (cover, gallery) = Gallery.normalize(image, images)
            items.upsert(
                ItemEntity(id.value, listId.value, title.trim(), note.trim(), cover?.value, null, Int.MAX_VALUE, now, now, null, gallery),
            )
            val ranking = currentRanking(listId)
            val updated = if (placeIndex == null) ranking else ranking.place(id, placeIndex.coerceIn(0, ranking.places.size), asTie)
            writeRanking(updated)
            touchList(listId)
            id
        }

    /**
     * Updates an entry. [images] is the full picture list in order; null keeps the current
     * pictures (a changed [image] then takes the old cover's slot).
     */
    suspend fun updateItem(id: ItemId, title: String, note: String, image: ImageId?, images: List<ImageId>? = null) {
        val current = items.get(id.value) ?: return
        val old = current.toModel()
        val list = images ?: Gallery.swapCover(old.images, old.image, image)
        val (cover, gallery) = Gallery.normalize(image, list)
        items.upsert(
            current.copy(title = title.trim(), note = note.trim(), imageId = cover?.value, gallery = gallery, updatedAt = clock.now()),
        )
        touchList(ListId(current.listId))
    }

    suspend fun getRanking(listId: ListId): Ranking = currentRanking(listId)

    /** Replaces the order of a list. The ranking must contain exactly the live entries. */
    suspend fun saveRanking(listId: ListId, ranking: Ranking) = db.withTransaction {
        val live = items.getActiveOfList(listId.value).map { ItemId(it.id) }.toSet()
        val given = ranking.toPositions().map { it.item }.toSet()
        require(live == given) { "Ranking does not match the list's entries" }
        writeRanking(ranking)
        touchList(listId)
    }

    /** Moves an entry to the trash; the remaining places close up. */
    suspend fun moveItemToTrash(id: ItemId) = db.withTransaction {
        val entity = items.get(id.value) ?: return@withTransaction
        items.setDeleted(id.value, clock.now(), clock.now())
        writeRanking(currentRanking(ListId(entity.listId)))
        touchList(ListId(entity.listId))
    }

    /** Restores an entry to its former place (or the end of the list if that no longer exists). */
    suspend fun restoreItem(id: ItemId) = db.withTransaction {
        val entity = items.get(id.value) ?: return@withTransaction
        val listId = ListId(entity.listId)
        val oldPlace = entity.place
        items.setDeleted(id.value, null, clock.now())
        items.setPosition(id.value, null, Int.MAX_VALUE)
        val ranking = currentRanking(listId)
        val restored = if (oldPlace == null) ranking else ranking.place(id, oldPlace.coerceAtMost(ranking.places.size))
        writeRanking(restored)
        touchList(listId)
    }

    suspend fun deleteForever(entry: TrashEntry) {
        when (entry) {
            is TrashEntry.List -> lists.deleteForever(entry.list.id.value)
            is TrashEntry.Item -> items.deleteForever(entry.item.id.value)
        }
        cleanupImages()
    }

    suspend fun emptyTrash() {
        purgeDeletedBefore(Long.MAX_VALUE)
    }

    /** Removes trash older than 30 days and image files nobody references any more. */
    suspend fun purgeExpired() = purgeDeletedBefore(clock.now() - Retention.TRASH_MILLIS)

    private suspend fun purgeDeletedBefore(before: Long) {
        db.withTransaction {
            items.purgeDeletedBefore(before)
            lists.purgeDeletedBefore(before)
        }
        cleanupImages()
    }

    suspend fun cleanupImages() {
        images.deleteAllExcept(items.referencedImageIds().map(::ImageId).toSet())
    }

    // ---------------------------------------------------------------- internals

    private suspend fun currentRanking(listId: ListId): Ranking = items.getActiveOfList(listId.value).toRanking()

    private suspend fun writeRanking(ranking: Ranking) {
        items.setPositions(ranking.toPositions().map { Triple(it.item.value, it.place, it.order) })
    }

    private suspend fun touchList(id: ListId) {
        val l = lists.get(id.value) ?: return
        lists.upsert(l.copy(updatedAt = clock.now()))
    }
}
