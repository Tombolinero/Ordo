package io.github.tombolinero.ordo.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Database schema. Rules for changes (see docs/DATA.md):
 * - Never rename or drop a column in place; add a new version + migration instead.
 * - Every version is exported to /schemas and covered by a migration test.
 */
@Entity(tableName = "lists", indices = [Index("deleted_at")])
data class ListEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    /** Stable colour key, see ListColor.key. */
    val color: String,
    val pinned: Boolean,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long?,
)

@Entity(
    tableName = "items",
    foreignKeys = [
        ForeignKey(
            entity = ListEntity::class,
            parentColumns = ["id"],
            childColumns = ["list_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("list_id"), Index("deleted_at")],
)
data class ItemEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "list_id") val listId: String,
    val title: String,
    val note: String,
    @ColumnInfo(name = "image_id") val imageId: String?,
    /** 0-based place index; tied entries share it. Null = not placed yet (inbox). */
    val place: Int?,
    /** Order within the place, or within the inbox. */
    @ColumnInfo(name = "place_order") val placeOrder: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long?,
)

/** Search result row. */
data class ItemWithListRow(
    @androidx.room.Embedded val item: ItemEntity,
    @ColumnInfo(name = "list_title") val listTitle: String,
)
