package io.github.tombolinero.ordo.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Guards the promise "updates never lose your lists".
 *
 * For every schema version ever shipped, a database is created in that exact version with
 * sample rows, migrated to the newest version and checked. When the schema changes:
 * 1. bump OrdoDatabase.VERSION, add a Migration to OrdoDatabase.MIGRATIONS,
 * 2. commit the newly exported schemas/<n>.json,
 * 3. add the new version to [shippedVersions] (no other change needed here).
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val shippedVersions = listOf(1)

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        OrdoDatabase::class.java,
    )

    @Test fun `every shipped schema migrates to the current version and keeps data`() {
        for (version in shippedVersions) {
            val name = "migration-$version.db"
            helper.createDatabase(name, version).apply {
                execSQL("INSERT INTO lists (id, title, description, color, pinned, sort_order, created_at, updated_at, deleted_at) VALUES ('l1', 'Beste Autos', '', 'magenta', 1, 0, 1, 2, NULL)")
                execSQL("INSERT INTO items (id, list_id, title, note, image_id, place, place_order, created_at, updated_at, deleted_at) VALUES ('i1', 'l1', 'Koenigsegg', 'Notiz', 'img1', 0, 0, 1, 2, NULL)")
                execSQL("INSERT INTO items (id, list_id, title, note, image_id, place, place_order, created_at, updated_at, deleted_at) VALUES ('i2', 'l1', 'Taycan', '', NULL, NULL, 0, 1, 2, NULL)")
                close()
            }
            val db = helper.runMigrationsAndValidate(name, OrdoDatabase.VERSION, true, *OrdoDatabase.MIGRATIONS)
            db.query("SELECT title, color, pinned FROM lists WHERE id = 'l1'").use {
                assertThat(it.moveToFirst()).isTrue()
                assertThat(it.getString(0)).isEqualTo("Beste Autos")
                assertThat(it.getString(1)).isEqualTo("magenta")
                assertThat(it.getInt(2)).isEqualTo(1)
            }
            db.query("SELECT id, note, image_id, place FROM items ORDER BY id").use {
                assertThat(it.count).isEqualTo(2)
                it.moveToFirst()
                assertThat(it.getString(1)).isEqualTo("Notiz")
                assertThat(it.getString(2)).isEqualTo("img1")
                assertThat(it.getInt(3)).isEqualTo(0)
                it.moveToNext()
                assertThat(it.isNull(3)).isTrue()
            }
            db.close()
        }
    }

    @Test fun `current version is listed as shipped`() {
        assertThat(shippedVersions.last()).isEqualTo(OrdoDatabase.VERSION)
    }
}
