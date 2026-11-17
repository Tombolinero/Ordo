package io.github.tombolinero.ordo.core.backup

import kotlinx.serialization.json.JsonPrimitive
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** The canonical sample used by round-trip and golden tests. Never change it once v1 shipped. */
object Fixtures {
    val image1 = "RIFF-fake-webp-koenigsegg".toByteArray()
    val image2 = "RIFF-fake-webp-taycan".toByteArray()

    val data = BackupData(
        lists = listOf(
            BackupList(
                id = "list-cars", title = "Beste Autos", description = "Meine Favoriten", color = "magenta",
                pinned = true, sortOrder = 0, createdAt = 1_700_000_000_000, updatedAt = 1_700_000_500_000,
                items = listOf(
                    BackupItem("i-jesko", "Koenigsegg Jesko", "Über 1.600 PS", "img-jesko.webp", 0, 0, 1_700_000_000_001, 1_700_000_000_001),
                    BackupItem("i-taycan", "Porsche Taycan", "Elektrisch, Fahrwerk top", "img-taycan.webp", 1, 0, 1_700_000_000_002, 1_700_000_000_002),
                    BackupItem("i-ioniq", "Hyundai Ioniq 5", "", null, 2, 0, 1_700_000_000_003, 1_700_000_000_003),
                    BackupItem("i-cla", "Mercedes-Benz CLA", "Gleichstand mit Ioniq", null, 2, 1, 1_700_000_000_004, 1_700_000_000_004),
                    BackupItem("i-polestar", "Polestar 2", "noch einordnen", null, null, 0, 1_700_000_000_005, 1_700_000_000_005),
                    BackupItem("i-old", "Alter Eintrag", "", null, 3, 0, 1_700_000_000_006, 1_700_000_000_006, deletedAt = 1_700_000_900_000),
                ),
            ),
            BackupList(
                id = "list-names", title = "Babynamen", color = "violet", sortOrder = 1,
                createdAt = 1_700_000_100_000, updatedAt = 1_700_000_100_000,
                items = listOf(BackupItem("i-emma", "Emma", "", null, 0, 0, 1_700_000_100_001, 1_700_000_100_001)),
            ),
        ),
        settings = mapOf("example.flag" to JsonPrimitive(true)),
    )

    val images = mapOf("img-jesko.webp" to image1, "img-taycan.webp" to image2)

    fun write(data: BackupData = Fixtures.data, appCode: Int = 10000): ByteArray = ByteArrayOutputStream().also { out ->
        BackupArchive.write(
            out, data, images.mapValues { (_, b) -> { ByteArrayInputStream(b) } },
            appVersionName = "1.0.0", appVersionCode = appCode,
            createdAt = Instant.parse("2026-11-17T12:00:00Z"),
        )
    }.toByteArray()

    /** Builds a ZIP by hand for malformed-input tests. */
    fun zip(vararg entries: Pair<String, ByteArray>): ByteArray = ByteArrayOutputStream().also { out ->
        ZipOutputStream(out).use { z ->
            entries.forEach { (n, b) ->
                z.putNextEntry(ZipEntry(n)); z.write(b); z.closeEntry()
            }
        }
    }.toByteArray()

    fun manifest(formatVersion: Int = 1, files: Map<String, String>, appCode: Int = 10000) =
        """{"format":"ordo-backup","formatVersion":$formatVersion,"appVersionCode":$appCode,"files":{${files.entries.joinToString(",") { "\"${it.key}\":\"${it.value}\"" }}}}""".toByteArray()
}
