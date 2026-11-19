package io.github.tombolinero.ordo.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Version history (never edit an old entry):
 *  1 – lists, items (places, ties, inbox, trash)
 *  2 – items.gallery (several pictures per entry, image_id stays the cover)
 */
@Database(
    entities = [ListEntity::class, ItemEntity::class],
    version = OrdoDatabase.VERSION,
    exportSchema = true,
)
abstract class OrdoDatabase : RoomDatabase() {
    abstract fun lists(): ListDao
    abstract fun items(): ItemDao

    companion object {
        const val VERSION = 2
        const val NAME = "ordo.db"

        /** Manual migrations, oldest first. Each one is covered by MigrationTest. */
        val MIGRATIONS: Array<androidx.room.migration.Migration> = arrayOf(MIGRATION_1_2)

        fun create(context: Context, name: String? = NAME): OrdoDatabase {
            val builder = if (name == null) {
                Room.inMemoryDatabaseBuilder(context, OrdoDatabase::class.java)
            } else {
                Room.databaseBuilder(context, OrdoDatabase::class.java, name)
            }
            // No fallbackToDestructiveMigration: a missing migration must fail loudly in tests,
            // never silently wipe the user's lists.
            return builder.addMigrations(*MIGRATIONS).build()
        }
    }
}
