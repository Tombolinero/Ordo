package io.github.tombolinero.ordo.core.model

import java.util.UUID

/** Globally unique ids (UUIDs) so data from different phones/backups can be merged safely. */
@JvmInline
value class ListId(val value: String) {
    companion object {
        fun new() = ListId(UUID.randomUUID().toString())
    }
}

@JvmInline
value class ItemId(val value: String) {
    companion object {
        fun new() = ItemId(UUID.randomUUID().toString())
    }
}

/** Name of an image file in the app's image store (without directory). */
@JvmInline
value class ImageId(val value: String) {
    companion object {
        fun new() = ImageId(UUID.randomUUID().toString())
    }
}
