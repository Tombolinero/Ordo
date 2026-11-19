package io.github.tombolinero.ordo.core.database

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** 1 → 2: entries can have several pictures. The existing picture becomes the only one. */
internal val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE items ADD COLUMN gallery TEXT NOT NULL DEFAULT ''")
        connection.execSQL("UPDATE items SET gallery = image_id WHERE image_id IS NOT NULL")
    }
}

/** Splits the stored gallery column; falls back to the cover for rows without one. */
fun galleryIds(gallery: String, cover: String?): List<String> {
    val ids = gallery.split(',').filter { it.isNotBlank() }
    return when {
        ids.isNotEmpty() -> ids
        cover != null -> listOf(cover)
        else -> emptyList()
    }
}

fun joinGallery(ids: List<String>): String = ids.distinct().joinToString(",")
