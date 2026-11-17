package io.github.tombolinero.ordo.core.data

import androidx.room.withTransaction
import io.github.tombolinero.ordo.core.backup.BackupArchive
import io.github.tombolinero.ordo.core.backup.BackupContent
import io.github.tombolinero.ordo.core.backup.BackupData
import io.github.tombolinero.ordo.core.backup.BackupItem
import io.github.tombolinero.ordo.core.backup.BackupList
import io.github.tombolinero.ordo.core.database.ItemEntity
import io.github.tombolinero.ordo.core.database.ListEntity
import io.github.tombolinero.ordo.core.database.OrdoDatabase
import io.github.tombolinero.ordo.core.model.Clock
import io.github.tombolinero.ordo.core.model.ImageId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.time.Instant

/**
 * Creates and restores `.ordobackup` files (format: docs/BACKUP-FORMAT.md).
 *
 * Restoring is all-or-nothing: the file is read and verified completely into a temporary
 * folder first; only then is the database changed, inside one transaction.
 */
class BackupService(
    private val db: OrdoDatabase,
    private val images: ImageStore,
    private val workDir: File,
    private val appVersionName: String,
    private val appVersionCode: Int,
    private val clock: Clock = Clock.System,
) {
    enum class Mode { Replace, Merge }

    data class Summary(val lists: Int, val items: Int, val images: Int, val createdAt: String, val fromNewerApp: Boolean)

    /** A backup that was read and verified, waiting to be applied. */
    class Prepared internal constructor(val content: BackupContent, internal val imageDir: File) {
        val summary: Summary
            get() = Summary(
                lists = content.data.lists.count { it.deletedAt == null },
                items = content.data.lists.filter { it.deletedAt == null }.sumOf { l -> l.items.count { it.deletedAt == null } },
                images = content.manifest.counts.images,
                createdAt = content.manifest.createdAt,
                fromNewerApp = content.fromNewerApp,
            )

        fun discard() {
            imageDir.deleteRecursively()
        }
    }

    suspend fun export(out: OutputStream) = withContext(Dispatchers.IO) {
        val data = snapshot()
        val files = images.allFiles()
        BackupArchive.write(
            out = out,
            data = data,
            images = files.mapValues { (_, f) -> { f.inputStream() } },
            appVersionName = appVersionName,
            appVersionCode = appVersionCode,
            createdAt = Instant.ofEpochMilli(clock.now()),
        )
    }

    /** Reads and verifies [input]. Throws BackupException with a precise reason when it cannot be used. */
    suspend fun prepare(input: InputStream): Prepared = withContext(Dispatchers.IO) {
        val dir = File(workDir, "restore-${clock.now()}").apply { deleteRecursively(); mkdirs() }
        try {
            val content = BackupArchive.read(input, appVersionCode) { name, stream ->
                File(dir, name).outputStream().use { stream.copyTo(it) }
            }
            Prepared(content, dir)
        } catch (e: Throwable) {
            dir.deleteRecursively()
            throw e
        }
    }

    /**
     * Applies a prepared backup. Before anything changes, the current state is saved as a
     * safety backup in [safetyDir] (the newest three are kept).
     */
    suspend fun apply(prepared: Prepared, mode: Mode, safetyDir: File) = withContext(Dispatchers.IO) {
        try {
            writeSafetyBackup(safetyDir)
            val data = prepared.content.data
            // Images first: they are only referenced once the transaction below commits.
            prepared.imageDir.listFiles().orEmpty().forEach { images.adopt(it.name, it) }
            db.withTransaction {
                when (mode) {
                    Mode.Replace -> replace(data)
                    Mode.Merge -> merge(data)
                }
            }
            images.deleteAllExcept(db.items().referencedImageIds().map(::ImageId).toSet())
        } finally {
            prepared.discard()
        }
    }

    private suspend fun writeSafetyBackup(dir: File) {
        dir.mkdirs()
        val file = File(dir, "Ordo-Sicherheit-${clock.now()}.${BackupArchive.FILE_EXTENSION}")
        file.outputStream().use { export(it) }
        dir.listFiles().orEmpty().sortedByDescending { it.lastModified() }.drop(3).forEach { it.delete() }
    }

    private suspend fun replace(data: BackupData) {
        db.items().deleteAll()
        db.lists().deleteAll()
        db.lists().upsertAll(data.lists.map { it.toEntity() })
        db.items().upsertAll(data.lists.flatMap { l -> l.items.map { it.toEntity(l.id) } })
    }

    /**
     * Merge rules:
     * - Lists/entries only in the backup are added.
     * - For the same id, the newer `updatedAt` wins for title, note, image, colour etc.
     * - The local order of an existing list is kept; entries that are new to it land in its
     *   "not placed yet" inbox, so nothing gets shuffled around unexpectedly.
     */
    private suspend fun merge(data: BackupData) {
        for (bl in data.lists) {
            val local = db.lists().get(bl.id)
            if (local == null) {
                db.lists().upsert(bl.toEntity())
                db.items().upsertAll(bl.items.map { it.toEntity(bl.id) })
                continue
            }
            if (bl.updatedAt > local.updatedAt) {
                db.lists().upsert(bl.toEntity().copy(sortOrder = local.sortOrder))
            }
            val localItems = db.items().getAllOfList(bl.id).associateBy { it.id }
            var inboxOrder = localItems.values.filter { it.place == null }.maxOfOrNull { it.placeOrder + 1 } ?: 0
            for (bi in bl.items) {
                val li = localItems[bi.id] ?: db.items().get(bi.id)
                if (li == null) {
                    db.items().upsert(bi.toEntity(bl.id).copy(place = null, placeOrder = inboxOrder++))
                } else if (bi.updatedAt > li.updatedAt) {
                    db.items().upsert(li.copy(title = bi.title, note = bi.note, imageId = bi.image?.removeSuffix(".webp"), updatedAt = bi.updatedAt, deletedAt = bi.deletedAt))
                }
            }
        }
    }

    private suspend fun snapshot(): BackupData {
        val allItems = db.items().getAll().groupBy { it.listId }
        return BackupData(
            lists = db.lists().getAll().sortedBy { it.sortOrder }.map { l ->
                BackupList(
                    id = l.id, title = l.title, description = l.description, color = l.color, pinned = l.pinned,
                    sortOrder = l.sortOrder, createdAt = l.createdAt, updatedAt = l.updatedAt, deletedAt = l.deletedAt,
                    items = allItems[l.id].orEmpty().sortedWith(compareBy({ it.place ?: Int.MAX_VALUE }, { it.placeOrder })).map { i ->
                        BackupItem(
                            id = i.id, title = i.title, note = i.note, image = i.imageId?.let { ImageStore.fileName(ImageId(it)) },
                            place = i.place, placeOrder = i.placeOrder, createdAt = i.createdAt, updatedAt = i.updatedAt, deletedAt = i.deletedAt,
                        )
                    },
                )
            },
        )
    }

    private fun BackupList.toEntity() = ListEntity(id, title, description, color, pinned, sortOrder, createdAt, updatedAt, deletedAt)

    private fun BackupItem.toEntity(listId: String) =
        ItemEntity(id, listId, title, note, image?.removeSuffix(".webp"), place, placeOrder, createdAt, updatedAt, deletedAt)
}
