package io.github.tombolinero.ordo.core.model

/**
 * Card colour of a list. [key] is what gets stored (database, backups), so it must never change;
 * new colours may be added, unknown keys fall back to [Graphite].
 */
enum class ListColor(val key: String) {
    Graphite("graphite"),
    Magenta("magenta"),
    Violet("violet"),
    Ruby("ruby"),
    Cobalt("cobalt"),
    Petrol("petrol"),
    ;

    companion object {
        fun fromKey(key: String?): ListColor = entries.firstOrNull { it.key == key } ?: Graphite
    }
}

/** Timestamps are epoch milliseconds (UTC). */
data class RankList(
    val id: ListId,
    val title: String,
    val description: String = "",
    val color: ListColor = ListColor.Graphite,
    val pinned: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)

data class RankItem(
    val id: ItemId,
    val listId: ListId,
    val title: String,
    val note: String = "",
    /** Cover picture, shown in the list. Always one of [images]. */
    val image: ImageId? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    /** All pictures in display order (only visible on the entry's page). */
    val images: List<ImageId> = listOfNotNull(image),
)

/** A list with its live entries and their order. */
data class RankedList(
    val list: RankList,
    val items: Map<ItemId, RankItem>,
    val ranking: Ranking,
) {
    fun item(id: ItemId): RankItem = items.getValue(id)

    /** The entries on place 1 (several when tied), or empty. */
    val top: List<RankItem> get() = ranking.places.firstOrNull()?.items?.map(::item).orEmpty()
}

/** Summary for the home screen. */
data class ListSummary(
    val list: RankList,
    val itemCount: Int,
    val tieCount: Int,
    val unrankedCount: Int,
    val topItem: RankItem?,
    /** Images of the first few places, for the little preview stack. */
    val previewImages: List<ImageId>,
)

/** A search hit across all lists. */
data class SearchHit(
    val item: RankItem,
    val list: RankList,
    /** 1-based place, or null when the entry is not placed yet. */
    val place: Int?,
)

/** Something in the trash. */
sealed interface TrashEntry {
    val deletedAt: Long

    data class List(val list: RankList, val itemCount: Int) : TrashEntry {
        override val deletedAt: Long get() = list.deletedAt ?: 0
    }

    data class Item(val item: RankItem, val listTitle: String) : TrashEntry {
        override val deletedAt: Long get() = item.deletedAt ?: 0
    }
}

object Retention {
    /** Entries stay in the trash for 30 days. */
    const val TRASH_MILLIS: Long = 30L * 24 * 60 * 60 * 1000
}
