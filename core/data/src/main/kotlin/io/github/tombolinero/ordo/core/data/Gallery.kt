package io.github.tombolinero.ordo.core.data

import io.github.tombolinero.ordo.core.database.joinGallery
import io.github.tombolinero.ordo.core.model.ImageId

/** Rules that keep cover and picture list consistent. */
internal object Gallery {
    /** The cover is always part of the list; without a cover the first picture is used. */
    fun normalize(cover: ImageId?, images: List<ImageId>): Pair<ImageId?, String> {
        var list = images.distinct()
        if (cover != null && cover !in list) list = listOf(cover) + list
        val c = cover ?: list.firstOrNull()
        return c to joinGallery(list.map { it.value })
    }

    /** Replaces [old] cover by [new] at the same position (or removes it when [new] is null). */
    fun swapCover(images: List<ImageId>, old: ImageId?, new: ImageId?): List<ImageId> {
        if (old == new) return images
        val i = images.indexOf(old)
        return when {
            i < 0 -> listOfNotNull(new) + images
            new == null -> images - old!!
            else -> images.toMutableList().also { it[i] = new }
        }
    }
}
