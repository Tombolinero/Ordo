package io.github.tombolinero.ordo.core.model

/**
 * The order of one list, independent of storage.
 *
 * A list is a sequence of [Place]s. A place normally holds one entry; when it holds several,
 * those entries share the place (a tie). Entries that were added but not placed yet live in
 * [unranked] (the "not yet placed" inbox at the end of the list).
 *
 * Places are counted densely (decision E3 A): after two entries on place 3 comes place 4.
 *
 * Every operation returns a new, normalised [Ranking]: no empty places, no duplicates.
 */
data class Ranking(
    val places: List<Place> = emptyList(),
    val unranked: List<ItemId> = emptyList(),
) {
    data class Place(val items: List<ItemId>) {
        init {
            require(items.isNotEmpty()) { "A place needs at least one entry" }
        }

        val isTie: Boolean get() = items.size > 1
    }

    init {
        val all = places.flatMap { it.items } + unranked
        require(all.size == all.toSet().size) { "An entry can only appear once in a ranking" }
    }

    val rankedCount: Int get() = places.sumOf { it.items.size }
    val totalCount: Int get() = rankedCount + unranked.size

    /** 1-based place number of [item], or null when it is not placed yet. */
    fun placeOf(item: ItemId): Int? = places.indexOfFirst { item in it.items }.takeIf { it >= 0 }?.plus(1)

    fun indexOfPlace(item: ItemId): Int = places.indexOfFirst { item in it.items }

    fun contains(item: ItemId): Boolean = indexOfPlace(item) >= 0 || item in unranked

    /** Moves the whole place at [from] so that it ends up at index [to]. */
    fun movePlace(from: Int, to: Int): Ranking {
        if (from == to) return this
        require(from in places.indices && to in places.indices)
        val list = places.toMutableList()
        val place = list.removeAt(from)
        list.add(to, place)
        return copy(places = list)
    }

    /** Puts all entries of place [source] onto place [target] (creates or extends a tie). */
    fun mergePlaces(source: Int, target: Int): Ranking {
        if (source == target) return this
        require(source in places.indices && target in places.indices)
        val list = places.toMutableList()
        list[target] = Place(list[target].items + list[source].items)
        list.removeAt(source)
        return copy(places = list)
    }

    /** Resolves the tie at [index]: every entry gets its own place, in the current order. */
    fun splitPlace(index: Int): Ranking {
        require(index in places.indices)
        val list = places.toMutableList()
        val place = list.removeAt(index)
        list.addAll(index, place.items.map { Place(listOf(it)) })
        return copy(places = list)
    }

    /** Takes [item] out of its tie and gives it its own place directly below. */
    fun detachFromTie(item: ItemId): Ranking {
        val index = indexOfPlace(item)
        require(index >= 0) { "$item is not placed" }
        val place = places[index]
        if (!place.isTie) return this
        val list = places.toMutableList()
        list[index] = Place(place.items - item)
        list.add(index + 1, Place(listOf(item)))
        return copy(places = list)
    }

    /** Adds a brand-new entry to the inbox. */
    fun addUnranked(item: ItemId): Ranking {
        require(!contains(item))
        return copy(unranked = unranked + item)
    }

    /**
     * Places [item] (new, from the inbox, or moved from elsewhere) as its own place at
     * [placeIndex], or onto the existing place [placeIndex] when [asTie] is true.
     */
    fun place(item: ItemId, placeIndex: Int, asTie: Boolean = false): Ranking {
        val without = remove(item)
        val list = without.places.toMutableList()
        if (asTie) {
            require(placeIndex in list.indices)
            list[placeIndex] = Place(list[placeIndex].items + item)
        } else {
            require(placeIndex in 0..list.size)
            list.add(placeIndex, Place(listOf(item)))
        }
        return without.copy(places = list)
    }

    /** Moves a placed entry back to the inbox. */
    fun unplace(item: ItemId): Ranking = remove(item).copy(unranked = remove(item).unranked + item)

    /** Removes [item] completely (e.g. moved to the trash). */
    fun remove(item: ItemId): Ranking = Ranking(
        places = places.mapNotNull { p -> (p.items - item).takeIf { it.isNotEmpty() }?.let(::Place) },
        unranked = unranked - item,
    )

    /** Flattens to storage rows: (item, placeIndex or null, index within place / inbox). */
    fun toPositions(): List<Position> =
        places.flatMapIndexed { placeIndex, place ->
            place.items.mapIndexed { i, id -> Position(id, placeIndex, i) }
        } + unranked.mapIndexed { i, id -> Position(id, null, i) }

    data class Position(val item: ItemId, val place: Int?, val order: Int)

    companion object {
        /** Rebuilds a ranking from stored positions. Gaps in place numbers are closed. */
        fun fromPositions(positions: List<Position>): Ranking {
            val placed = positions.filter { it.place != null }
                .groupBy { it.place!! }
                .toSortedMap()
                .values
                .map { group -> Place(group.sortedBy { it.order }.map { it.item }) }
            val inbox = positions.filter { it.place == null }.sortedBy { it.order }.map { it.item }
            return Ranking(placed, inbox)
        }
    }
}
