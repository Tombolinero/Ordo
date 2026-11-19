package io.github.tombolinero.ordo.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ListDao {
    @Query("SELECT * FROM lists WHERE deleted_at IS NULL ORDER BY pinned DESC, sort_order ASC, created_at DESC")
    fun observeActive(): Flow<List<ListEntity>>

    @Query("SELECT * FROM lists WHERE id = :id")
    fun observe(id: String): Flow<ListEntity?>

    @Query("SELECT * FROM lists WHERE id = :id")
    suspend fun get(id: String): ListEntity?

    @Query("SELECT * FROM lists")
    suspend fun getAll(): List<ListEntity>

    @Query("SELECT * FROM lists WHERE deleted_at IS NOT NULL ORDER BY deleted_at DESC")
    fun observeDeleted(): Flow<List<ListEntity>>

    @Query("SELECT COALESCE(MIN(sort_order), 0) FROM lists")
    suspend fun minSortOrder(): Int

    @Upsert
    suspend fun upsert(list: ListEntity)

    @Upsert
    suspend fun upsertAll(lists: List<ListEntity>)

    @Query("UPDATE lists SET deleted_at = :deletedAt, updated_at = :now WHERE id = :id")
    suspend fun setDeleted(id: String, deletedAt: Long?, now: Long)

    @Query("UPDATE lists SET sort_order = :order WHERE id = :id")
    suspend fun setSortOrder(id: String, order: Int)

    @Query("DELETE FROM lists WHERE id = :id")
    suspend fun deleteForever(id: String)

    @Query("DELETE FROM lists WHERE deleted_at IS NOT NULL AND deleted_at < :before")
    suspend fun purgeDeletedBefore(before: Long): Int

    @Query("DELETE FROM lists")
    suspend fun deleteAll()
}

@Dao
interface ItemDao {
    @Query("SELECT * FROM items WHERE list_id = :listId AND deleted_at IS NULL")
    fun observeActiveOfList(listId: String): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE list_id = :listId AND deleted_at IS NULL")
    suspend fun getActiveOfList(listId: String): List<ItemEntity>

    @Query("SELECT * FROM items WHERE list_id = :listId")
    suspend fun getAllOfList(listId: String): List<ItemEntity>

    /** All live entries of all live lists (home screen summaries, widget). */
    @Query(
        "SELECT items.* FROM items JOIN lists ON lists.id = items.list_id " +
            "WHERE items.deleted_at IS NULL AND lists.deleted_at IS NULL",
    )
    fun observeAllActive(): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items")
    suspend fun getAll(): List<ItemEntity>

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun get(id: String): ItemEntity?

    @Query("SELECT * FROM items WHERE id = :id")
    fun observe(id: String): Flow<ItemEntity?>

    @Upsert
    suspend fun upsert(item: ItemEntity)

    @Upsert
    suspend fun upsertAll(items: List<ItemEntity>)

    @Query("UPDATE items SET place = :place, place_order = :order WHERE id = :id")
    suspend fun setPosition(id: String, place: Int?, order: Int)

    @Query("UPDATE items SET deleted_at = :deletedAt, updated_at = :now WHERE id = :id")
    suspend fun setDeleted(id: String, deletedAt: Long?, now: Long)

    @Query("DELETE FROM items WHERE id = :id")
    suspend fun deleteForever(id: String)

    /**
     * Case-insensitive search in titles and notes of live entries in live lists.
     * [pattern] must already be a LIKE pattern with `\` as escape character.
     */
    @Query(
        "SELECT items.*, lists.title AS list_title FROM items JOIN lists ON lists.id = items.list_id " +
            "WHERE items.deleted_at IS NULL AND lists.deleted_at IS NULL " +
            "AND (items.title LIKE :pattern ESCAPE '\\' OR items.note LIKE :pattern ESCAPE '\\') " +
            "ORDER BY items.title COLLATE NOCASE LIMIT 200",
    )
    fun search(pattern: String): Flow<List<ItemWithListRow>>

    /** Entries in the trash whose list is still alive (deleted lists show up as a whole). */
    @Query(
        "SELECT items.*, lists.title AS list_title FROM items JOIN lists ON lists.id = items.list_id " +
            "WHERE items.deleted_at IS NOT NULL AND lists.deleted_at IS NULL ORDER BY items.deleted_at DESC",
    )
    fun observeDeleted(): Flow<List<ItemWithListRow>>

    @Query("DELETE FROM items WHERE deleted_at IS NOT NULL AND deleted_at < :before")
    suspend fun purgeDeletedBefore(before: Long): Int

    @Query("SELECT image_id AS cover, gallery FROM items WHERE image_id IS NOT NULL OR gallery != ''")
    suspend fun imageRows(): List<ImageRow>

    /** Every picture still used by some entry (live or in the trash), covers and galleries. */
    suspend fun referencedImageIds(): List<String> =
        imageRows().flatMap { galleryIds(it.gallery, it.cover) + listOfNotNull(it.cover) }.distinct()

    @Query("DELETE FROM items")
    suspend fun deleteAll()

    @Transaction
    suspend fun setPositions(positions: List<Triple<String, Int?, Int>>) {
        positions.forEach { (id, place, order) -> setPosition(id, place, order) }
    }
}
