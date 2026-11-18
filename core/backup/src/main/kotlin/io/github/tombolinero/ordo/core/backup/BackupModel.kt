package io.github.tombolinero.ordo.core.backup

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/*
 * The backup file format. Full description and change rules: docs/BACKUP-FORMAT.md
 *
 * Compatibility rules, in short:
 *  - Adding a feature = adding OPTIONAL fields with defaults. formatVersion stays the same.
 *    Older apps ignore unknown fields, newer apps fill in defaults for missing ones.
 *  - Only a structural change (renaming, moving, changing the meaning of a field) bumps
 *    formatVersion and requires a step in BackupMigrations.
 *  - Never remove a field's default value.
 */

@Serializable
data class BackupManifest(
    val format: String = FORMAT_ID,
    val formatVersion: Int,
    val appVersionName: String = "",
    val appVersionCode: Int = 0,
    /** ISO-8601 instant, UTC. */
    val createdAt: String = "",
    val counts: Counts = Counts(),
    /** SHA-256 (hex) of every other file in the archive, keyed by path. */
    val files: Map<String, String> = emptyMap(),
) {
    @Serializable
    data class Counts(val lists: Int = 0, val items: Int = 0, val images: Int = 0)

    companion object {
        const val FORMAT_ID = "ordo-backup"
    }
}

@Serializable
data class BackupData(
    val lists: List<BackupList> = emptyList(),
    /** App settings by key. Unknown keys are kept as they are. */
    val settings: Map<String, JsonElement> = emptyMap(),
)

@Serializable
data class BackupList(
    val id: String,
    val title: String,
    val description: String = "",
    val color: String = "graphite",
    val pinned: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val deletedAt: Long? = null,
    val items: List<BackupItem> = emptyList(),
)

@Serializable
data class BackupItem(
    val id: String,
    val title: String,
    val note: String = "",
    /** File name inside images/, or null. */
    val image: String? = null,
    /** 0-based place; tied entries share it. Null = not placed yet. */
    val place: Int? = null,
    val placeOrder: Int = 0,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val deletedAt: Long? = null,
)

/** Result of reading a backup. Images were already streamed to the caller's sink. */
data class BackupContent(
    val manifest: BackupManifest,
    val data: BackupData,
    /** True when the backup was written by a newer app version than the one reading it. */
    val fromNewerApp: Boolean,
)

sealed class BackupException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    /** Not an Ordo backup at all (wrong file). */
    class NotABackup(cause: Throwable? = null) : BackupException("Not an Ordo backup", cause)

    /** Damaged: checksum mismatch, missing or malformed parts. */
    class Corrupted(detail: String, cause: Throwable? = null) : BackupException("Backup is damaged: $detail", cause)

    /** Written by a newer app with an incompatible format: update the app first. */
    class NewerFormat(val formatVersion: Int) : BackupException("Backup format $formatVersion is newer than supported")

    /** Exceeds the safety limits (size, number of files). */
    class TooLarge(detail: String) : BackupException("Backup too large: $detail")
}
